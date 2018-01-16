(ns fsm.core)

(defn- third
  ^{:doc "Returns the third value in the tuple (transition state)"
    :attribution "https://github.com/jebberjeb/fsmviz/blob/master/src/fsmviz/core.cljc"}
  [coll]
  (nth coll 2))

(defn- term-states
  ^{:doc "Returns states which have no outbound transitions."
    :attribution "https://github.com/jebberjeb/fsmviz/blob/master/src/fsmviz/core.cljc"}
  [tuples]
  (clojure.set/difference (set (map third tuples)) (set (map first tuples))))

(defn- map->tuples
  ^{:doc "Returns a collection of [from via to] tuples representing the FSM."
    :attribution "https://github.com/jebberjeb/fsmviz/blob/master/src/fsmviz/core.cljc"}
  [state-map]
  (mapcat (fn [[from m]]
            (map (fn [[trans to]]
                   [from trans to])
                    m))
          state-map))

(defn- get-termination-states
  "Returns a set of termination states"
  [fsm-spec]
  (-> fsm-spec
      map->tuples
      term-states))

(defn- is-termination-state?
  "Checks if the provided state is a termination state"
  [fsm-spec state]
  (-> fsm-spec
      get-termination-states
      (contains? state)))

(defn- next-state
  "Processes the next state in the state machine"
  [state-transition-functions
   fsm
   {:keys [transition event] :as context}]
  (let [next-transition (get-in fsm [transition event])
        transition-fn (when next-transition
                        (next-transition state-transition-functions))]

    (when (and (not transition-fn)
               (not (is-termination-state? fsm next-transition)))
      (throw (ex-info "Invalid finite state machine implementation"
                      {:fsm-spec fsm
                       :exit-state next-transition
                       :termination-states (get-termination-states fsm)})))

    (if transition-fn
      (next-state state-transition-functions
                  fsm
                  (transition-fn (assoc context
                                        :transition
                                        next-transition)))
      context)))

(defn initialize-state-machine
  [initial-state
   state-transition-functions
   fsm-spec]
  (next-state state-transition-functions
              fsm-spec
              {:state initial-state
               :transition :start
               :event :init
               :error nil}))

(defn fsm-handler
  "Formats FSM response to be consumed by service handlers"
  ([result]
   (fsm-handler result {:throw-with
                        (fn [{:keys [client-message status metadata]
                             :or {status 400 metadata nil}}]
                          (throw (ex-info "FSM Handler Error"
                                          {:type :fsm-handler-error
                                           :message client-message
                                           :status-code status
                                           :metadata metadata})))}))
  ([result {:keys [throw-with] :as options}]
   (let [{:keys [error state]} result]
     (if error
       (throw-with error)
       state))))
