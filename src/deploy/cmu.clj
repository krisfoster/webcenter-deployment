(ns deploy.cmu
  (:gen-class)
  (:use clojure.repl)
  (:use clojure.inspector)
  (:use clojure.pprint)
  (:require clojure.reflect)
  (:require clojure.string)
  (:require clojure.java.browse)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string])
  (:import (java.util.zip ZipInputStream))
  (:import (java.io File InputStreamReader))
  (:import (oracle.stellent.ridc IdcClient IdcContext IdcClientManager))
  (:import (oracle.stellent.ridc.model DataBinder TransferFile)))

;;;
;;; Consts.
;;;
(def http-lib "http.library")
(def http-lib-apache "apache4")
(def idc-service "IdcService")
(def idc-service-cmu-upload "CMU_UPLOAD_BUNDLE")
(def idc-service-cmu-delete "CMU_DELETE_BUNDLE")
(def idc-service-cmu-import "CMU_UPDATE_AND_CREATE_ACTION")
(def idc-service-cmu-list-actions "CMU_LIST_ACTIVE_ACTIONS")
(def idc-service-cmu-create-export-template "createExportTemplate")
(def idc-service-cmu-force-bundle-overwrite "forceBundleOverwrite")
(def idc-service-cmu-bundle-name "bundleName")
(def idc-service-cmu-task-name "TaskName")
(def idc-service-cmu-is-import "isImport")
(def idc-service-cmu-overwrite-dups "isOverwrite")
(def idc-service-cmu-continue-on-error "isContinueOnError")
(def idc-service-cmu-section-item-list "sectionItemList")
(def empty-str "")
(def idc-service-param-true "1")

// set the SSL socket options
;;;config.setKeystoreFile("keystore/client_keystore");  //location of keystore file
;;;config.setKeystorePassword ("password");      // keystore password
;;;config.setKeystoreAlias("SecureClient");  //keystore alias
;;;config.setKeystoreAliasPassword("password");

;;;
;;; Extracting task name from a CMU bundle
;;;
(defn- get-value
  ([s] (get-value s #"\s*=\s*"))
  ([s re]
     (let [[_ value] (clojure.string/split s re)]
       (clojure.string/trim value))))

(defn- entries [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn- get-zipped-file-contents [zf e line-filter]
  (let [zis (.getInputStream zf e)
        is  (InputStreamReader. zis "UTF-8")]
    (with-open [rdr (clojure.java.io/reader is)]
      (get-value (first (filter line-filter (line-seq rdr)))))))

(defn- extract-value-from-zip [fileName file-name-filter line-filter]
  (with-open [z (java.util.zip.ZipFile. fileName)]
    (let [files (filter file-name-filter (entries z))
          task-file (first files)
          task-name (get-zipped-file-contents z task-file line-filter)]
      task-name)))

(defn get-task-name [bundle]
  (extract-value-from-zip bundle
                      #(= "task.hda" (.getName %))
                      #(.startsWith % "TaskName=")))

;;;
;;; SSL stuff
;;;
(defn is-url-ssl? [url]
  (.startsWith url "https://"))

;; config.setKeystoreFile("ketstore/client_keystore");  //location of keystore file
;; config.setKeystorePassword ("password");      // keystore password
;; config.setKeystoreAlias("SecureClient");  //keystore alias
;; config.setKeystoreAliasPassword("password");  //password for keystore alias
#_(defn config-ssl
  "Configures the SSL connection stuff - namely the "
  [config]
  (.setKeystoreFile ))

;;;
;;; Connection heper functions
;;;
(defn client [conn-url]
  (-> (new IdcClientManager)
      (.createClient conn-url)))

(defn context [user pass]
  (new IdcContext user pass))

(defn get-binder [client]
  (.createBinder client))

(defn set-property [client k v]
  (let [config (.getConfig client)]
    (.setProperty config k v)))

(defn set-socket-timeout [client timeout]
  (let [config (.getConfig client)]
    (.setSocketTimeout config timeout)))

(defn set-conn-size [client size]
  (let [config (.getConfig client)]
    (.setConnectionSize config size)))

(defn use-http [client]
  (set-property client http-lib http-lib-apache))

(defn get-response-binder [resp]
  (.getResponseAsBinder resp))

(defn get-rs [binder rs-name]
  (.getResultSet binder rs-name))



;;;
;;; CMU Service wrappers
;;;

;;;
(defn cmu-upload-bundle
  "Performs an upload of a CMU bundle to a Webcenter instance over HTTP"
  [file-path conn-url user pass]
  (let [file (File. file-path)]
    (if (and (.exists file) (.isFile file))
      (let [clnt (client conn-url)
            cxt  (context user pass)]
        (use-http clnt)
        (let [binder (get-binder clnt)
              tf (new TransferFile file)]
          (.putLocal binder idc-service idc-service-cmu-upload)
          (.putLocal binder idc-service-cmu-create-export-template "1")
          (.putLocal binder idc-service-cmu-force-bundle-overwrite "1")
          (.addFile binder idc-service-cmu-bundle-name tf)
          (.sendRequest clnt cxt binder)))
      (throw (Exception. (str "CMU file does not exist:: " file-path))))))

;;;
(defn cmu-delete-bundle
  "Deletes an uploaded CMU bundle on a Webcenter instance over HTTP"
  [task-name conn-url user pass]
  (let [clnt (client conn-url)
        cxt  (context user pass)]
    (use-http clnt)
    (let [binder (get-binder clnt)]
      (.putLocal binder idc-service idc-service-cmu-delete)
      (.putLocal binder idc-service-cmu-task-name task-name)
      (.sendRequest clnt cxt binder))))

;;;
(defn cmu-import-bundle
  "Imports an uploaded CMU bundle on a Webcenter instance over HTTP"
  [task-name conn-url user pass]
  (let [clnt (client conn-url)
        cxt  (context user pass)]
    (use-http clnt)
    (let [binder (get-binder clnt)]
      (.putLocal binder idc-service idc-service-cmu-import)
      (.putLocal binder idc-service-cmu-task-name task-name)
      (.putLocal binder idc-service-cmu-is-import idc-service-param-true)
      (.putLocal binder idc-service-cmu-overwrite-dups idc-service-param-true)
      (.putLocal binder idc-service-cmu-continue-on-error idc-service-param-true)
      (.putLocal binder idc-service-cmu-section-item-list empty-str)
      (.sendRequest clnt cxt binder))))

#_(defn show-props [obj]
 (->> (clojure.reflect/reflect obj)
       :members
       ;; (filter #(.startsWith (str (:name %)) "put"))
       (clojure.pprint/pprint)))

(defn browse-idc
  "Opens a browser that points the the given service"
  [conn-url service m]
  (let [url (str conn-url "?IdcService=" service
                 (doseq [[k v] m]
                   (str "&" k "=" v)))]
    (clojure.java.browse/browse-url url)))

(defn browse-latest-cmu-actions
  "Opens a browser on the latest CMU actions page. Useful for checking to see if
   an import has worked. "
  [conn-url]
  (browse-idc conn-url "CMU_LIST_ACTIVE_ACTIONS" {}))

;;;
;;; Command line integration and main method
;;;

;;;
;;; Command line arguments that are accepted
;;;
(def cli-options
  [["-cmu" "--cmu CMU_FILE" "File Path to CMU bundle"]
   ["-i" "--import" "Indicates that we should import the CMU bundle"]
   ["-url" "--url URL" "The connection URL to the Webcenter Server. of the form :: http://<server>:<port>/cs/idcplg"]
   ["-u" "--user USER" "The user to connect to Webccnter as - typicaly 'weblogic'"]
   ["-p" "--pass PASS" "The user password"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Uploads and then imports a CMU bunlde into a Webcenter content instance"
        ""
        "Usage: program-name [options]"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn exit [status msg]  (println msg)
  (System/exit status))


;;;
;;; Main method
;;;
(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options) (exit 0 (usage summary))
     (nil? (:cmu options)) (exit 1 (usage summary))
     (nil? (:url options)) (exit 1 (usage summary))
     (nil? (:user options)) (exit 1 (usage summary))
     (nil? (:pass options)) (exit 1 (usage summary)))
    (let [cmu (:cmu options)
          file (File. cmu)
          conn-url (:url options)
          import (if (:import options) :import true)
          user (:user options)
          pass (:pass options)
          task-name (get-task-name (.getPath file))]
      (println "Uploading & importing CMU Bunlde....")
      (println "CMU file  :: " file)
      (println "URL       :: " conn-url)
      (println "User      :: " user)
      (println "Password  :: " pass)
      (println "Importing :: " import)
      (println "Task name :: " task-name)
      (if (and (.exists file) (.isFile file))
        (do
          (println "CMU file path:: " (.getPath file))
          ;; Upload the bundle
          (cmu-upload-bundle (.getPath file) conn-url user pass)
          ;; Import the bundle
          (cmu-import-bundle task-name conn-url user pass)
          ;;
          (browse-latest-cmu-actions conn-url)
          (println "CMU bundle uploading and importing. Please check the opened browser to monitor the import progress."))
        (exit 1 (str "Failed to locate CMU file:: " cmu))))))
