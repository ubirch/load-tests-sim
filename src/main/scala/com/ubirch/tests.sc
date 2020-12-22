val maxClients = (0 until 4)
val devices = List(1,2,3, 4,5,6,7,8,9,10)

val buckets = devices.size / maxClients.size

devices.slice(buckets*0, buckets*1)
devices.slice(buckets*1, buckets*2)
devices.slice(buckets*2, buckets*3)
devices.slice(buckets*3, buckets*4)

require(maxClients.size < devices.size)
val rr=(0 to maxClients.size).map { i =>
  devices.slice(buckets * i, buckets * (i+1))
}

println(rr)
