(ns trump-bot.core
  (:gen-class)
  (:require [clojure.set]
            [twitter.api.restful :as twitter]
            [twitter.oauth :as twitter-oauth]
            [environ.core :refer [env]]
            [overtone.at-at :as overtone]))


(defn -main
  "I don't do a whole lot ... yet."
  [& args])


(def twitter-credentials
  (twitter-oauth/make-oauth-creds (env :app-consumer-key)
                                  (env :app-consumer-secret)
                                  (env :user-access-token)
                                  (env :user-access-secret)))

(defn markov-chain
  "Creates a Markov chain.
  Expects: A sequence of Paritions with count 3."
  [word-transitions]
  (reduce (fn
          [destination source]
          (merge-with clojure.set/union destination
                      (let [[a b c] source]
                        {[a b] (if c #{c} #{})})))
          {}
          word-transitions))

(defn text->word-chain
  "Takes an unformatted string and returns a Markov Chain"
  [string]
  (let [words (clojure.string/split string #"[\s|\n]")
        word-transitions (partition-all 3 1 words)]
    (markov-chain word-transitions)))



(defn chain->text
  "Takes a seq of words, a result-chain, and converts to a string"
  [chain]
  (apply str (interpose " " chain)))

(defn walk-chain
  "Takes a start prefix, a Markov-chain, an initial result, and a character limit and produces a result-chain"
  [prefix chain result limit]
  (let [suffixes (get chain prefix)]
    (if (empty? suffixes)
      result
      (let [suffix (first (shuffle suffixes))
            new-prefix [(last prefix) suffix]
            result-with-spaces (chain->text result)
            result-char-count (count result-with-spaces)
            suffix-char-count (inc (count suffix))
            new-result-char-count (+ result-char-count suffix-char-count)]
        (if (>= new-result-char-count limit)
          result
          (recur new-prefix chain (conj result suffix) limit))))))

(defn process-file [filename]
  (text->word-chain
   (slurp (clojure.java.io/resource filename))))


(defn generate-text
  [start-phrase word-chain limit]
  (let [prefix (clojure.string/split start-phrase #" ")
        result-chain (walk-chain prefix word-chain prefix limit)
        result-text (chain->text result-chain)]
    result-text))

(defn end-at-last-punctuation [text]
  (let [trimmed-to-last-punct (apply str (re-seq #"[\s\w]+[^.!?,]*[.!?,]" text))
        trimmed-to-last-word (apply str (re-seq #".*[^a-zA-Z]+" text))
        result-text (if (empty? trimmed-to-last-punct)
                      trimmed-to-last-word
                      trimmed-to-last-punct)
        cleaned-text (clojure.string/replace result-text #"[,| ]$" ".")]
    (clojure.string/replace cleaned-text #"\"" "'")))

(def prefix-list
  ["I love"])

(defn tweet
  [limit source seed]
  (let [text (generate-text seed (process-file source) limit)]
    (end-at-last-punctuation text)))

(defn tweet-trump
  []
  (let [text (generate-text (-> prefix-list shuffle first) (process-file "quotes.txt")
              )]
    (end-at-last-punctuation text)))

(defn get-ran-prefix
  [filename]
  (let [prefix 
        (-> (process-file filename) seq shuffle first first)]
    (apply str (interpose " " prefix))))

(defn post-to-twitter
  []
  (let [tweet (tweet 140)]
    (println "Tweet: " tweet)
    (println "Character count:" (count tweet))
    (when (not-empty tweet)
      (try (twitter/statuses-update :oauth-creds twitter-credentials
                                    :params {:status tweet})
           (catch Exception e (println "Error: " (.getMessage e)))))))
