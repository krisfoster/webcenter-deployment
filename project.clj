(defproject deploy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [oracle/ridc "11.1.1"]
                 [org.apache.httpcomponents/httpclient "4.3.3"]
                 [org.apache.httpcomponents/httpmime "4.3.3"]
                 [org.apache.httpcomponents/httpcore "4.3.2"]
                 [commons-codec/commons-codec "1.9"]
                 [commons-httpclient/commons-httpclient "3.1"]
                 [commons-logging/commons-logging "1.1.3"]
                 ;;[org.apache.httpcomponents/httpclient "3.1"]
                 ;;[org.apache.httpcomponents/httpmime "4.1.1"]
                 ;;[org.apache.httpcomponents/httpcore "4.1"]
                 ;;[commons-codec/commons-codec "1.2"]
                 ;;[commons-logging/commons-logging "1.0.4"]
                 [cider/cider-nrepl "0.6.1-SNAPSHOT"]
                 [org.clojure/tools.nrepl "0.2.3"
                  :exclusions ([org.clojure/clojure])]
                 [org.clojure/tools.cli "0.3.1"]
]
  :user {:plugins [[lein-ritz "0.6.0"]]}

  :repl-options {:nrepl-middleware
                 [cider.nrepl.middleware.classpath/wrap-classpath
                  cider.nrepl.middleware.complete/wrap-complete
                  cider.nrepl.middleware.info/wrap-info
                  cider.nrepl.middleware.inspect/wrap-inspect
                  cider.nrepl.middleware.stacktrace/wrap-stacktrace
                  ;;; cider.nrepl.middleware.trace/wrap-trace
                  ]}
  :main deploy.cmu)
