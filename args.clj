(defn manage-pc
  "This style takes a first arg of a data dictionary (destured)
  followed by optional keyword arguments"
  [{:keys [os host] :as data} & {:keys [scan upgrade]
                                 :or   {scan true upgrade false}
                                 :as   opts}]
  {:pre [os host scan]} ;; required keys
  (printf "\nDATA:\nos: %s\ndata: %s\nOPTIONS:\nscan: %s\nupgrade: %s\nopts: %s"
          os data scan upgrade opts))

(manage-pc {:os "linux" :host "seneca"} :scan :true :upgrade "maybe" :blah :arg)
