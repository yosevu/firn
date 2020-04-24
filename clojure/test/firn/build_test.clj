(ns firn.build-test
  (:require [firn.build :as sut]
            [me.raynes.fs :as fs]
            [clojure.test :as t]
            [firn.config :as config]))
           

(def test-dir  "test/firn/demo_org")
(def firn-dir  (str test-dir "/_firn"))

(defn- delete-firn-dir
  []
  (fs/delete-dir firn-dir))

(t/deftest new-site
  ;; The _firn site shouldn't exist yet when new-site is called.
  (t/testing "The _firn site shouldn't exist yet"
    (t/is (= false (fs/exists? firn-dir))))

  (sut/new-site {:dir-files test-dir})

  (t/testing "Creates a new site with the proper structure."
    (doseq [file-path sut/default-files
            :let [file-path-full     (str firn-dir "/" file-path)
                  file-string-length (-> file-path-full slurp count)]]
      (t/is (= true (fs/exists? file-path-full)))
      ;; each file should have a string of contents > 0
      (t/is (> file-string-length 0))))

  (t/testing "Trying to create again when _firn already exists should return false"
    (t/is (= false (sut/new-site {:dir-files test-dir}))))

  (delete-firn-dir))


(t/deftest setup
  ;; setup requires that you have the _firn site in place; so:
  (sut/new-site {:dir-files test-dir})

  (let [config       (config/prepare test-dir)
        setup-config (sut/setup config)]
    (t/testing "expect org-files, layouts and partials to be a key in config"
      (t/is (= true (empty? (config :org-files))))
      (t/is (= true (empty? (config :layouts))))
      (t/is (= true (empty? (config :partials))))

      (t/is (= true (not (empty? (setup-config :org-files)))))
      (t/is (= true (not (empty? (setup-config :layouts)))))
      (t/is (= true (not (empty? (setup-config :partials))))))

    (t/testing "_site should be created"
      (t/is (= true (fs/exists? (setup-config :dir-site))))))

  (delete-firn-dir))


(config/prepare test-dir)
