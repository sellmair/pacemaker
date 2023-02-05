package io.sellmair.broadheart

object Me {
    val user = User(
        uuid = randomUUID(),
        name = "Sebastian Sellmair",
        imageUrl = null
    )

    /* 140 is the default limit when unchanged */
    var myLimit: HeartRate = HeartRate(140)

    var myHeartRate: HeartRate? = null
}