val prop by extra("hello")
val prop1 by extra(prop)
val prop2 by extra("goodbye")
val prop3 by extra("${prop2}")
val prop4 by extra(prop3)
val prop5 by extra(prop4)
val prop6 by extra(prop5)
