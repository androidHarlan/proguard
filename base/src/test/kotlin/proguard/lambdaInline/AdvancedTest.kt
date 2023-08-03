package proguard.lambdaInline

import compareOutputAndMainInstructions
import io.kotest.core.spec.style.FreeSpec
import proguard.testutils.KotlinSource

class AdvancedTest: FreeSpec ({
    "three lambda functions with two of same size and one different" {
        //setup
        val code = KotlinSource( //create java file
            "Main.kt",
            """
        fun test(a: (Int) -> Int) {
            println(a(12))
        }
        fun a(b: (String) -> Int) {
            println(b("Hello World!") + 1)
        }
        
        fun test1(a: (Int) -> Int, b: (String, Boolean, Int) -> Boolean, c: (Int) -> Int) {
            println(a(3))
            println(b("Hello", true, 0))
            println(c(5))
        }
        
        
        fun main() {
            var index = 0
            while (index < 100) {
                index += 1
                test { a: Int -> (a * a) + 1 }
                a { b: String ->
                        var cnt = 0
                        var i = 0
                        while (i < b.length) {
                            cnt += b[i].code
                            i++
                        }
                        cnt
                }
                test1({ a: Int -> a * a }, { a : String, b : Boolean, c : Int -> a.uppercase(); b}, {a : Int -> a+a})
            }
        }
        """
        )

        compareOutputAndMainInstructions(
            code,
            listOf(
                "iconst_0",
                "istore_0",
                "iload_0",
                "bipush",
                "if_icmpge",
                "iinc",
                "nop",
                "invokestatic",
                "invokestatic",
                "invokestatic",
                "goto",
                "return"
            )
        )
    }

    "switch case, unit lambda, multiple call to the same lambda" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
        fun test(a: (Int) -> Unit) {
            a(1)
            a(2)
            a(3)
        }

        fun main() {
            test { 
                when(it) {
                    1 -> println("Hello")
                    2 -> println("World")
                    3 -> println("!")
                }
            }
        }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Casting basic types after the lambda call" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
        fun test(a: (Int) -> Int) {
            println(a(12))

            var i: Int = 1
            var f: Float = i.toFloat()
            println(f + 4.3)
        }
        
        fun main() {
            test { a: Int -> (a * a) + 1 }
        }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Casting types before the lambda call" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
        fun test(a: (Int) -> Int) {
            val a = test1(5)
            if (a is Int) {
                println(a + 1)
            } else if (a is Boolean) {
                println(a and true)
            }
        
            println(a(12))
        }
        
        fun test1(a:Any): Any {
            if (a is Int) {
                return true
            } else if (a is Boolean) {
                return 7
            } else {
                return 'a'
            }
        }
        
        fun main() {
            test { a: Int -> (a * a) + 1 }
        }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Casting types after the lambda call" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
        fun test(a: (Int) -> Int) {
            println(a(12))

            val a = test1(5)
            if (a is Int) {
                println(a + 1)
            } else if (a is Boolean) {
                println(a and true)
            }
        }
        
        fun test1(a:Any): Any {
            if (a is Int) {
                return true
            } else if (a is Boolean) {
                return 7
            } else {
                return 'a'
            }
        }
        
        fun main() {
            test { a: Int -> (a * a) + 1 }
        }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Casting types between lambda calls" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
        fun test(a: (Int) -> Int) {
            println(a(12))

            val a = test1(5)
            if (a is Int) {
                println(a + 1)
            } else if (a is Boolean) {
                println(a and true)
            }

            println(a(12))
        }
        
        fun test1(a:Any): Any {
            if (a is Int) {
                return true
            } else if (a is Boolean) {
                return 7
            } else {
                return 'a'
            }
        }
        
        fun main() {
            test { a: Int -> (a * a) + 1 }
        }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Casting types inside lambda call" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
            fun test(a: (Any) -> Int) {
                println(a(1))
            }
            
            fun main() {
                test { a: Any ->  if (a is Int) {
                    println(a + 1)
                } else if (a is Boolean) {
                    println(a and true)
                }; 1}
            }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Casting types inside lambda call + other arg" {
        val code = KotlinSource( //create java file
            "Main.kt",
            """
            fun test(a: (Any, Int) -> Int) {
                println(a(1, 2))
            }
            
            fun main() {
                test { a: Any, b: Int ->  if (a is Int) {
                    println(a + 1)
                } else if (a is Boolean) {
                    println(a and true)
                }; b}
            }
        """
        )

        compareOutputAndMainInstructions(code, listOf("invokestatic", "return"))
    }

    "Recursive function should not be inlined" {
        val code = KotlinSource(
            "Main.kt",
        """
        fun test(i: Int, f: (Int) -> Unit) {
            if (i >= 0) {
                f(i)
                test(i - 1, f)
            }
        }
        
        fun test2(f: (Int) -> Int) {
            println(f(2))
        }
        
        fun main() {
            test(5) { println(it * 2) }
            test2 { it * 3}
        }
        """.trimIndent())

        compareOutputAndMainInstructions(code, listOf("iconst_5", "getstatic", "checkcast", "invokestatic", "invokestatic", "return"), false)
    }


})