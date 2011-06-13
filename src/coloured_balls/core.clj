(ns coloured-balls.core
  (:use [rosado.processing]
        [rosado.processing.applet])
  (:gen-class))

;; here's a function which will be called by Processing's (PApplet)
;; draw method every frame. Place your code here. If you eval it
;; interactively, you can redefine it while the applet is running and
;; see effects immediately
(defstruct ball :x :y :vx :vy :red :blue :green :radius)

(def screen-size 800)

(def initial-max-size 30)

(def no-balls 100)

(defn draw-ball [ball]
	(fill (:red ball) (:green ball) (:blue ball))
	(ellipse (:x ball) (:y ball) (:radius ball) (:radius ball)))

(defn make-ball []
  (struct-map ball :x (rand-int screen-size) :y (rand-int screen-size)
	      :vx (- (* 2 (rand-int 5)) 5) :vy (-  (* 2 (rand-int 5)) 5)
	      :red (rand-int 256) :blue (rand-int 256) :green (rand-int 256) :radius (rand-int initial-max-size)))


(def ball-state (atom (take no-balls (repeatedly make-ball))))

(defn move [ball]
  (update-in
   (update-in ball [:x] #(+ % (:vx ball)))
   [:y]
   #(+ % (:vy ball))))


(defn collide [ball]
  (let [bounce-positive (fn [key] (update-in ball [key] #(Math/abs %)))
	bounce-negative (fn [key] (update-in ball [key] #(- (Math/abs %))))]     
    (cond
     (< (:x ball) (/ (:radius ball) 2))  (bounce-positive :vx)
     (< (- screen-size (:x ball)) (/ (:radius ball) 2)) (bounce-negative :vx)
     (< (:y ball) (/ (:radius ball) 2)) (bounce-positive :vy) 
     (< (- screen-size (:y ball)) (/ (:radius ball) 2)) (bounce-negative :vy)
     :otherwise ball)))

(defn collides? [b1 b2]
  (let [dx (- (:x b1) (:x b2))
	dy (- (:y b1) (:y b2))]
    (< (Math/sqrt (+ (* dx dx) (* dy dy)))
       (/ (+ (:radius b1) (:radius b2)) 2))))

(defn vortho [[x y]]
  [(- y) x])

(defn vadd [[u v] [x y]]
  [(+ u x) (+ v y)])

(defn vsub [[u v] [x y]]
  [(- u x) (- v y)])

(defn vlen [[x y]]
  (Math/sqrt (+ (* x x) (* y y))))

(defn vunit [[x y]]
  [(/ x (vlen [x y]))
   (/ y (vlen [x y]))])

(defn vmul [a [x y]]
  [(* a x) (* a y)])

(defn reflect [ball opposite]
  (let [pb [(:x ball) (:y ball)]
	po [(:x opposite) (:y opposite)]
	move [(:vx ball) (:vy ball)]
	connector (vunit (vsub po pb))
	disp (vsub connector (vunit move))
	newpb (vmul (- (vlen move)) (vunit (vadd connector disp)))]
    (assoc ball :vx (first newpb) :vy (second newpb))))
    
(defn eat [ball other-ball]
(let [ball-radius (:radius ball)
  other-ball-radius (:radius other-ball)]
  (assoc ball :radius (if (< ball-radius other-ball-radius) (- ball-radius (quot ball-radius 4)) 
(+ ball-radius (quot other-ball-radius 4))))))

(defn mutual-collisions [balls]
  (map
   (fn [b]
     (let [crash (some #(if (and (not= % b) (collides? % b)) % nil) balls)]
      (if (not (nil? crash))
        (eat (reflect b crash) crash)
	 b)))
   balls))

(defn remove-empty-balls [balls]
  (filter #(< 0 (:radius %)) balls)
)

(defn draw
  "Example usage of with-translation and with-rotation."
  []

  (swap! ball-state #(map (comp move collide) %))
  (swap! ball-state mutual-collisions)
  (swap! ball-state remove-empty-balls)
  
  
  (background 226)
  (doall
   (map draw-ball @ball-state)))

(defn setup []
  "Runs once."
  (smooth)
  (no-stroke)
  (fill 226)
  (framerate 20))

;; Now we just need to define an applet:

(defapplet balls :title "Coloured balls"
  :setup setup :draw draw :size [screen-size screen-size])

(run balls)
