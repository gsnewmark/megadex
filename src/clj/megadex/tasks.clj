(ns megadex.tasks
  (:require [megadex.crawl.p4g :as p4g]))

(defn generate-p4g-fixture
  []
  (let [skills-source (p4g/skills-raw-html)
        persona-list-source (p4g/personas-list-raw-html)
        skills
        (-> skills-source
            p4g/skills-html
            p4g/skills
            p4g/skills-fixture)
        personas
        (-> persona-list-source
            p4g/personas-list-html
            p4g/personas-list
            p4g/personas-raw-html
            p4g/personas-html
            p4g/personas
            (p4g/personas-fixture (p4g/skills-ids skills)))]
    (spit "resources/public/fixtures/p4g.edn"
          (with-out-str (pr (into [] (concat skills personas)))))))
