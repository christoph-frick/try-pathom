(ns try-pathom.core
  (:require [clojure.core.async :refer [<!!]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]))

; fake: this should optimize to fetch a batch
(defn db-lookup [db id-key params]
  (if (sequential? params)
    (mapv #(db-lookup db id-key %) params)
    (get db (get params id-key))))

(pc/defresolver brand-resolver [{:keys [:brand/db] :as env} params]
  {::pc/input #{:brand/id}
   ::pc/output [:brand/name]
   ::pc/batch? true}
  (db-lookup db :brand/id params))

(pc/defresolver product-resolver [{:keys [:product/db] :as env} params]
  {::pc/input #{:product/id}
   ::pc/output [:product/name :brand/id]
   ::pc/batch? true}
  (db-lookup db :product/id params))

(pc/defresolver cart-resolver [env _]
  {::pc/output [{::cart [:product/id :cart/amount]}]}
  (select-keys env [::cart]))

(def parser
  (p/parser
   {::p/env {::p/reader [p/map-reader
                         pc/reader2
                         pc/open-ident-reader
                         p/env-placeholder-reader]
             ::p/placeholder-prefixes #{">"}}
    ::p/mutate pc/mutate
    ::p/plugins [(pc/connect-plugin {::pc/register [brand-resolver
                                                    product-resolver
                                                    cart-resolver]})
                 p/error-handler-plugin
                 p/trace-plugin]}))

#_(parser {::p/fail-fast? true
         :brand/db {1 {:brand/name "Riegele"}}
         :product/db {1 {:product/name "Augustus" :brand/id 1}
                      2 {:product/name "Noctus" :brand/id 1}
                      3 {:product/name "Auris" :brand/id 1}}
         ::cart [{:cart/amount 1 :product/id 1}
                 {:cart/amount 4 :product/id 2}
                 {:cart/amount 3 :product/id 3}]}
        [{::cart [:product/name :brand/name :cart/amount]}])
