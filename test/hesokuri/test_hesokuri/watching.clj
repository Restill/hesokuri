; Copyright (C) 2013 Google Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;    http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns hesokuri.test-hesokuri.watching
  (:import [java.io File FileOutputStream])
  (:use clojure.test
        hesokuri.test-hesokuri.temp
        hesokuri.watching))

; TODO: This test runs very slowly. Figure out a way to mock out the
;     java.nio.file file watching system so that this is really a unit test.
(deftest test-watcher-for-dir
  (let [changed-files (atom clojure.lang.PersistentQueue/EMPTY)
        temp-dir (create-temp-dir)

        watcher
        (watcher-for-dir temp-dir (fn [path] (swap! changed-files #(conj % path))))

        wait-for-change
        (fn [file]
          (let [file (File. file)]
            (loop [total-sleeps 0]
              (cond
               (> total-sleeps 400)
               (throw (IllegalStateException.
                       (str "Watcher did not report change in time: " file
                            " (changed-files: " (seq @changed-files) ")")))

               (= (peek @changed-files) file)
               (swap! changed-files pop)

               :else
               (do
                 (Thread/sleep 100)
                 (recur (inc total-sleeps)))))))]
    (Thread/sleep 1000)

    (->> "file1" (File. temp-dir) .createNewFile)
    (wait-for-change "file1")

    (->> "file2" (File. temp-dir) .createNewFile)
    (wait-for-change "file2")

    (-> (File. temp-dir "file1") (FileOutputStream. true) (.write 42))
    (wait-for-change "file1")

    (-> (File. temp-dir "file2") (FileOutputStream. true) (.write 1011))
    (wait-for-change "file2")

    (Thread/sleep 100)
    (is (= [] @changed-files))))
