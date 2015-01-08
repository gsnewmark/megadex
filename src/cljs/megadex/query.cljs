(ns megadex.query)

(def persona
  '[:find ?name ?arcana ?level
          ?st ?ma ?en ?ag ?lu
          ?inherit ?resists ?block ?absorbs ?reflects ?weak

    :in $ ?normalized-name

    :where
    [?pid :persona/normalized-name ?normalized-name]
    [?pid :persona/name ?name]
    [?pid :persona/arcana ?aid]
    [?aid :arcana/name ?arcana]
    [?pid :persona/level ?level]

    [?pid :persona.stats/st ?st]
    [?pid :persona.stats/ma ?ma]
    [?pid :persona.stats/en ?en]
    [?pid :persona.stats/ag ?ag]
    [?pid :persona.stats/lu ?lu]

    [?pid :persona.elements/inherit ?inherit]
    [?pid :persona.elements/resists ?resists]
    [?pid :persona.elements/block ?block]
    [?pid :persona.elements/absorbs ?absorbs]
    [?pid :persona.elements/reflects ?reflects]
    [?pid :persona.elements/weak ?weak]])

(def skills-of-persona
  '[:find ?name ?level ?effect ?cost

    :in $ ?persona-normalized

    :where
    [?pid :persona/normalized-name ?persona-normalized]
    [?psid :persona.skill/persona ?pid]
    [?psid :persona.skill/skill ?sid]
    [?psid :persona.skill/level-acquired ?level]
    [?sid :skill/name ?name]
    [?sid :skill/effect ?effect]
    [?sid :skill/cost ?cost]])

(def arcanas-with-personas
  '[:find ?arcana ?arcana-normalized ?persona ?persona-normalized ?level

    :where
    [?aid :arcana/name ?arcana]
    [?aid :arcana/normalized-name ?arcana-normalized]
    [?pid :persona/arcana ?aid]
    [?pid :persona/name ?persona]
    [?pid :persona/normalized-name ?persona-normalized]
    [?pid :persona/level ?level]])

(def arcana-with-personas
  '[:find ?persona-normalized ?level

    :in $ ?normalized-name

    :where
    [?aid :arcana/normalized-name ?normalized-name]
    [?pid :persona/arcana ?aid]
    [?pid :persona/normalized-name ?persona-normalized]
    [?pid :persona/level ?level]])

