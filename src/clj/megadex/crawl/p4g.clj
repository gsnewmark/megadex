(ns megadex.crawl.p4g
  (:require [clojure.string :as cstr]
            [net.cgrand.enlive-html :as html]))

(defn sanitize-text [text]
  (or (first (re-seq #"[A-Za-z0-9 %,~_\-.\"':]+" text)) ""))

(defn html->text [html]
  ((comp cstr/trim sanitize-text html/text) html))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn- mapify [part-by value-fn coll]
  (->> coll
       (partition-by part-by)
       (partition 2)
       (map (fn [[k v]] [(html/text (first k)) (value-fn v)]))
       (into {})))

(defn skills-raw-html []
  (fetch-url "http://megamitensei.wikia.com/wiki/List_of_Persona_4_Skills"))

(defn skills-html [raw-html]
  (let [types (into #{} (map html->text
                             (html/select raw-html [:h2 :span.mw-headline])))]
    (->> #{[:h2 :span.mw-headline] [:h3 :span.mw-headline] [:table.p4]}
         (html/select raw-html)
         (mapify (fn [e] (types (html->text e)))
                 (fn [v]
                   (if (> (count v) 1)
                     (mapify #(= "mw-headline" (get-in % [:attrs :class]))
                             identity v)
                     v))))))

(defn parse-table [table-html]
  (let [[headers-html & vals-html] (html/select table-html [:tr])
        headers (map html->text (html/select headers-html [:th]))]
    (map (fn [e] (->> (html/select e #{[:th] [:td]})
                      (map html->text)
                      (interleave headers)))
         vals-html)))

(declare skills)

(defn parse-skill [[type skills-html]]
  [type (if (map? skills-html)
          (skills skills-html)
          (let [specs (map html->text (html/select skills-html [:th]))
                skills (->> (parse-table skills-html)
                            (map (fn [[name-title name & rest]]
                                   [name (apply hash-map rest)])))]
            (into [] skills)))])

(defn skills [skills-html]
  (into {} (map parse-skill skills-html)))

(defn personas-list-raw-html []
  (fetch-url "http://megamitensei.wikia.com/wiki/List_of_Persona_4_Personas"))

(defn personas-list-html [raw-html]
  (let [types (into #{} (map html->text
                             (html/select raw-html [:h2 :span.mw-headline])))]
    (->> #{[:h2 :span.mw-headline] [:table.p4]}
         (html/select raw-html)
         (mapify (fn [e] (types (html->text e))) identity))))

(defn parse-personas-list [[arcana list-html]]
  [arcana
   (into {}
         (mapcat (fn [e] (->> (html/select e #{[:th] [:a]})
                              (partition 2)
                              (map (fn [[level link]]
                                     [(html->text link)
                                      {:level (html->text level)
                                       :link (get-in link [:attrs :href])}]))))
                 (rest (html/select list-html [:tr]))))])

(defn personas-list [personas-list-html]
  (into {} (map parse-personas-list personas-list-html)))

(defn persona-raw-html [persona-path]
  (fetch-url (str "http://megamitensei.wikia.com" persona-path)))

(defn persona-html [persona-raw-html]
  (let [[stats elemental skills]
        (html/select persona-raw-html [:table.customtable.p4])]
    {:stats stats :elemental elemental :skills skills}))

(defn persona [persona-html]
  (let [{:keys [stats elemental skills]} persona-html
        to-map #(->> % parse-table first (apply hash-map))]
    {:stats (to-map stats)
     :elemental (to-map elemental)
     :skills (->> (html/select skills [:tr])
                  rest ;; drop title row
                  parse-table
                  (map (fn [[name-title name & rest]]
                         [name (apply hash-map rest)]))
                  (into {}))}))

(defn map-over-persona [f personas-seq]
  (into {}
        (map (fn [[arcana personas]] [arcana (into {} (map f personas))])
             personas-seq)))

(defn personas-raw-html [personas-list]
  (map-over-persona
   (fn [[name {:keys [link] :as info}]]
     [name (-> info
               (dissoc :link)
               (assoc :raw-html (persona-raw-html link)))])
   personas-list))

(defn personas-html [personas-raw-html]
  (map-over-persona
   (fn [[name {:keys [raw-html] :as info}]]
     [name (-> info
               (dissoc :raw-html)
               (assoc :html (persona-html raw-html)))])
   personas-raw-html))

(defn personas [personas-html]
  (map-over-persona
   (fn [[name {:keys [html] :as info}]]
     [name (-> info
               (dissoc :html)
               (merge (persona html)))])
   personas-html))

(defn normalize-keyword [k]
  (-> k cstr/lower-case (cstr/replace #"\.$" "")))

(defn to-fixture-props [namespace m]
  (map (fn [[k v]] [(keyword namespace (normalize-keyword k)) v]) m))

(defn skills-fixture
  ([skills] (let [id (atom 0)] (skills-fixture #(swap! id inc) skills)))
  ([next-id skills] (skills-fixture next-id nil skills))
  ([next-id supertype-id skills]
   (mapcat
    (fn [[type ss]]
      (let [current-type-id (next-id)

            skill-type-datom
            (merge {:db/id current-type-id :skill-type/name type}
                   (if supertype-id
                     {:skill-type/super-type supertype-id}
                     {}))

            skill-datom
            (fn [[name specs]]
              (into {:db/id (next-id)
                     :skill/type current-type-id
                     :skill/name name}
                    (to-fixture-props "skill" specs)))]
        (concat
         [skill-type-datom]
         (if (map? ss)
           (skills-fixture next-id current-type-id ss)
           (map skill-datom ss)))))
    skills)))

(defn skills-ids
  ([skills-fixture]
   (->> skills-fixture
        (filter :skill/name)
        (map (juxt :skill/name :db/id))
        (into {}))))

(defn personas-fixture
  ([personas skills-ids]
   (let [id (atom 0)] (personas-fixture #(swap! id inc) personas skills-ids)))
  ([next-id personas skills-ids]
   (mapcat
    (fn [[arcana ps]]
      (let [current-arcana-id (next-id)

            arcana-type-datom
            {:db/id current-arcana-id :arcana/name arcana}

            persona-datom
            (fn [[name {:keys [skills elemental stats level]}]]
              (let [persona-id (next-id)
                    basic-persona-data
                    {:db/id persona-id
                     :persona/arcana current-arcana-id
                     :persona/name name
                     :persona/level level}]
                (conj
                 (->> skills
                      (map
                       (fn [[skill-name skill]]
                         (when-let [skill-id (get skills-ids skill-name)]
                           {:db/id (next-id)
                            :persona.skill/persona persona-id
                            :persona.skill/level-acquired (get skill "Level")
                            :persona.skill/skill skill-id})))
                      (remove nil?))
                 (into basic-persona-data
                       (concat
                        (to-fixture-props "persona.elements" elemental)
                        (to-fixture-props "persona.stats"
                                  (dissoc stats "Arcana" "Level")))))))]
        (concat [arcana-type-datom] (mapcat persona-datom ps))))
    personas)))

(def schema
  {:persona/arcana {:db/valueType :db.type/ref}
   :persona.skill/persona {:db/valueType :db.type/ref}
   :persona.skill/skill {:db/valueType :db.type/ref}
   :skill-type/super-type {:db/valueType :db.type/ref}
   :skill/type {:db/valueType :db.type/ref}})

