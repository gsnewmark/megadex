(ns megadex.query)

(def persona
  '[:find ?arcana ?level
          ?st ?ma ?en ?ag ?lu
          ?inherit ?resists ?block ?absorbs ?reflects ?weak

    :in $ ?name

    :where
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

(def arcanas-with-personas
  '[:find ?arcana ?persona ?level

    :where
    [?aid :arcana/name ?arcana]
    [?pid :persona/arcana ?aid]
    [?pid :persona/name ?persona]
    [?pid :persona/level ?level]])

(def arcana-with-personas
  '[:find ?persona ?level

    :in $ ?name

    :where
    [?aid :arcana/name ?name]
    [?pid :persona/arcana ?aid]
    [?pid :persona/name ?persona]
    [?pid :persona/level ?level]])

