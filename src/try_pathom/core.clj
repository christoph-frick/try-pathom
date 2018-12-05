(ns try-pathom.core
  (:require [clojure.core.async :refer [<!!]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]))

(pc/defresolver person-resolver [env {:keys [person/id] :as params}]
  {::pc/input #{:person/id}
   ::pc/output [:person/first-name :person/last-name {:person/address [:address/id]}]}
  {:person/first-name "Tom"
   :person/last-name "Harris"
   :person/address {:address/id 1}})

(pc/defresolver person-full-name-resolver [env {:person/keys [first-name last-name] :as params}]
  {::pc/input #{:person/first-name :person/last-name}
   ::pc/output [:person/full-name]}
  {:person/full-name (str first-name " " last-name)})

(pc/defresolver address-resolver [env {:keys [address/id] :as params}]
  {::pc/input #{:address/id}
   ::pc/output [:address/city :address/state]}
  {:address/city "Salem"
   :address/state "MA"})

(def my-resolvers [person-resolver 
                   person-full-name-resolver
                   address-resolver])

(def parser
  (p/parser
   {::p/env {::p/reader [p/map-reader
                         pc/reader2
                         pc/open-ident-reader
                         p/env-placeholder-reader]
             ::p/placeholder-prefixes #{">"}}
    ::p/mutate pc/mutate
    ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                 p/error-handler-plugin
                 p/trace-plugin]}))

#_(parser {} [{[:person/id 1] [:person/full-name {:person/address [:address/city]}]}])
