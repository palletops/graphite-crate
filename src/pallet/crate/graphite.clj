;;; Copyright 2012, 2013 Hugo Duncan.
;;; All rights reserved.

;; http://geek.michaelgrace.org/2011/09/how-to-install-graphite-on-ubuntu/
;; http://linuxracker.com/2012/03/31/setting-up-graphite-server-on-debian-squeeze/
;; http://stackoverflow.com/questions/11491268/install-pycairo-in-virtualenv

(ns pallet.crate.graphite
  "A pallet crate to install and configure graphite"
  (:require
   [clojure.string :refer [join]]
   [pallet.action :refer [with-action-options]]
   [pallet.actions
    :refer [assoc-settings directory exec-checked-script exec-script group
            packages remote-directory remote-file sed service symbolic-link
            user]
    :rename {user user-action group group-action
             assoc-settings assoc-settings-action
             service service-action}]
   [pallet.api :refer [plan-fn] :as api]
   [pallet.config-file.format :refer [sectioned-properties]]
   [pallet.crate
    :refer [defplan assoc-settings defmethod-plan get-settings
            get-node-settings group-name nodes-with-role target-id]]
   [pallet.crate-install :as crate-install]
   [pallet.crate.graphite.config :refer [carbon-config]]
   [pallet.script.lib :refer [pid-root log-root config-root user-home]]
   [pallet.stevedore :refer [fragment script]]
   [pallet.utils :refer [apply-map]]
   [pallet.version-dispatch
    :refer [defmulti-version-plan defmethod-version-plan]]))


(def ^{:doc "Flag for recognising changes to configuration"}
  graphite-config-changed-flag "graphite-config")

;;; # Settings
(defn default-settings []
  {:version "0.9.10"
   :user "graphite"
   :owner "graphite"
   :group "graphite"
   :plugin-dir "/var/lib/graphite"
   :home "/opt/graphite"
   :config-dir "/opt/graphite/conf"
   :carbon-config carbon-config
   :storage-schemas (array-map
                     :carbon {:pattern "^carbon\\."
                              :retentions "60:90d"}
                     :default_1min_for_1day {:pattern ".*"
                                             :retentions "60s:1d"})
   :storage-aggregation {}
   :relay-rules nil
   :aggregation-rules nil
   :webapp-bind-address "127.0.0.0:8080"
   :service "graphite"})

;;; At the moment we just have a single implementation of settings,
;;; but this is open-coded.
(defmulti-version-plan settings-map [version settings])

(defmethod-version-plan
    settings-map {:os :linux}
    [os os-version version settings]
  (cond
   (:install-strategy settings) settings
   :else (assoc settings :install-strategy ::virtualenv)))

(defplan settings
  "Settings for graphite"
  [{:keys [user owner group dist dist-urls cloudera-version version
           instance-id]
    :as settings}]
  (let [settings (merge (default-settings) settings)
        settings (settings-map (:version settings) settings)]
    (assert (:install-strategy settings) "No install strategy specified.")
    (assoc-settings :graphite settings {:instance-id instance-id})))

;;; # Install
(defmethod-plan crate-install/install ::virtualenv
  [facility instance-id]
  (let [{:keys [user owner group src-dir home url] :as settings}
        (get-settings facility {:instance-id instance-id})]
    (packages
     :yum ["python-virtualenv" "python-pip" "python-dev"]
     :apt ["python-virtualenv" "python-setuptools" "python-dev"
           "python-cairo"]
     :aptitude ["python-virtualenv" "python-setuptools" "python-dev"
                "python-cairo"])
    (directory home :owner user :group group)
    (exec-checked-script
     "Allow pallet script execution as other users"
     ("chmod" "711" .))
    (with-action-options
      {:sudo-user user :script-dir (fragment (~user-home ~user))}
      (exec-checked-script
       "Install graphite with virtualenv"
       (when (not (file-exists? (str ~home "/bin/python")))
         ("virtualenv" ~home))
       ("source" (str ~home "/bin/activate"))
       ("export" (set! HOME ~home))
       ("pip" -q install whisper)
       ("pip" -q install carbon)
       ("pip" -q install django)
       ("pip" -q install django-tagging)
       ("pip" -q install graphite-web)
       ("easy_install" gunicorn)
       ;; TODO
       ;; edit local_settings.py to enable DATABASE
       ))
    (remote-file (str home "/lib/python2.7/site-packages/cairo")
                 :link "/usr/lib/python2.7/dist-packages/cairo")))

(defplan install
  "Install graphite."
  [{:keys [instance-id]}]
  (let [settings (get-settings :graphite {:instance-id instance-id})]
    (crate-install/install :graphite instance-id)))


;; (defplan install-gunicorn
;;   "Install gunicorn"
;;   [{:keys [instance-id] :as options}]
;;   [{:keys [home service user]}
;;    (get-settings :graphite {:instance-id instance-id})]
;;   (with-action-options {:script-dir home :sudo-user user}
;;     (exec-checked-script
;;      (str "Graphite " daemon " " action)
;;      ("easy_install" gunicorn))))

;;; # User
(defplan user
  "Create the graphite user"
  [{:keys [instance-id] :as options}]
  (let [{:keys [user owner group home]} (get-settings :graphite options)]
    (group-action group :system true)
    (when (not= owner user)
      (user-action owner :group group :system true))
    (user-action
     user :group group :system true :create-home true :shell :bash)))


;;; # Configuration
(defplan config-file
  "Helper to write config files"
  [{:keys [owner group config-dir] :as settings} filename file-source]
  (apply
   remote-file (str config-dir "/" filename)
   :flag-on-changed graphite-config-changed-flag
   :owner owner :group group
   (apply concat file-source)))

(defplan configure
  "Write all config files"
  [{:keys [instance-id] :as options}]
  (let [{:keys [carbon-config storage-aggregation storage-schemas home user
                owner group]
         :as settings}
        (get-settings :graphite options)]
    (config-file settings "carbon.conf"
                 {:content (sectioned-properties carbon-config)})
    (config-file settings "storage-schemas.conf"
                 {:content (sectioned-properties storage-schemas)})
    (config-file settings "storage-aggregation.conf"
                 {:content (sectioned-properties storage-aggregation)})
    (remote-file
     (str home "/webapp/graphite/local_settings.py")
     :content
     "DATABASES = {
    'default': {
        'NAME': '/opt/graphite/storage/graphite.db',
        'ENGINE': 'django.db.backends.sqlite3',
        'USER': '',
        'PASSWORD': '',
        'HOST': '',
        'PORT': ''
    }
}"
     :owner owner :group group)
    (sed (str home "/webapp/graphite/app_settings.py")
         {"SECRET_KEY = ''" "SECRET_KEY = 'abcdef'"})
    (with-action-options {:sudo-user user
                          :script-dir (str home "/webapp/graphite/")}
      (exec-checked-script
       "Initialise database"
       ("source" (str ~home "/bin/activate"))
       ("python" manage.py syncdb "--noinput")))))

(defplan service
  "Control a graphite service.

   Specify `:if-config-changed true` to make actions conditional on a change in
   configuration.

   Other options are as for `pallet.action.service/service`. The service
   name is looked up in the request parameters."
  [daemon {:keys [action if-config-changed if-flag instance-id]
           :or {action :start}
           :as options}]
  (let [{:keys [home service user]}
        (get-settings :graphite {:instance-id instance-id})
        options (if if-config-changed
                  (assoc options :if-flag graphite-config-changed-flag)
                  options)]
    (with-action-options {:script-dir home :sudo-user user}
      (exec-checked-script
       (str "Graphite " daemon " " action)
       ("source" (str ~home "/bin/activate"))
       (if (not ((str ~home "/bin/" ~daemon ".py") status))
         ("nohup" (str ~home "/bin/" ~daemon ".py") ~(name action)))))))

(defplan web-server
  "Start a graphite web service"
  [{:keys [instance-id] :as options}]
  (let [{:keys [home service user group webapp-bind-address]}
        (get-settings :graphite {:instance-id instance-id})]
    (with-action-options {:script-dir home}
      (exec-script
       ;; TODO work out how to check if this is already running
       ;; (str "Graphite web server")
       ("source" (str ~home "/bin/activate"))
       (when (not (pipe ("ps" ax) ("grep" gunicorn_django) ("grep" -v grep)))
         ("("
          ("nohup"
           (str ~home "/bin/gunicorn_django")
           -u ~user -g ~group -b ~webapp-bind-address
           (str "--access-logfile=" ~home "/storage/log/webapp/gunicorn.log")
           (str "--pythonpath=" ~home "/webapp")
           "graphite.settings")
          "& )")
         ("sleep" 5))))))                 ; allow sub-process to start

(defn server-spec
  "Returns a server-spec that installs and configures graphite"
  [settings & {:keys [instance-id] :as options}]
  (api/server-spec
   :phases
   {:settings (plan-fn
                (pallet.crate.graphite/settings (merge settings options)))
    :install (plan-fn
               (user options)
               (install options))
    :configure (plan-fn
                 (configure options)
                 (service "carbon-cache" options)
                 (web-server options))
    :start (plan-fn
             (service "carbon-cache" options)
             (web-server options))}))
