(defproject trump-bot "0.1.0-SNAPSHOT"
  :description "A Markov Bot"
  :url "http://drinkjava.github.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [overtone/at-at "1.2.0"]
                 [twitter-api "0.7.8"]
                 [environ "1.0.0"]]
  :main trump-bot.core
  :min-lein-version "2.0.0"
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:dev {:plugins [[com.jakemccrary/lein-test-refresh "0.7.0"]]}})
