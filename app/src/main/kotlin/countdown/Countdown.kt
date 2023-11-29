package countdown

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.*

fun main() = (solve(983, 10, 9, 8, 7, 5, 6)
    ?.toStringWithValue() ?: "No solution").let(::println)

fun Expression.toStringWithValue(): String =
    "$this = $value"

fun solve(target: Int, vararg numbers: Int): Expression? {
    println("Target: $target, source numbers: ${numbers.joinToString(",")}")
    var answer: Expression? = null
    val time = measureTime {
        runBlocking(Dispatchers.Default) {
            try {
                answer = numbers
                    .map(::NumberExpression)
                    .permute()
                    .flatMap(::expressions)
                    .filter(differenceFrom(target).within(10))
                    .parallelReduce(comparer(target))
            } catch (_: NoSuchElementException) {
            }
        }
    }.inWholeMilliseconds
    (answer?.toStringWithValue() ?: "No solution found").let(::println)
    println("Completed in ${time}ms")
    return answer
}

fun <T> List<T>.permute(): Sequence<List<T>> = distinct().asSequence().flatMap {v -> 
    sequenceOf(listOf(v)) + remove(v).permute().map {listOf(v) + it}
}

fun expressions(exprs: List<Expression>): Sequence<Expression> =
    if (exprs.size < 2) exprs.asSequence()
    else exprs.indices.drop(1).asSequence()
            .map(exprs::splitAt)
            .map {it.map(::expressions)}
            .flatMap {(left, right) -> 
                left
                .flatMap(::combinersUsing)
                .flatMap {c -> right.mapNotNull(c)}
            }

fun <T> List<T>.splitAt(pos: Int): List<List<T>> =
    listOf(take(pos), drop(pos))

fun <T> List<T>.remove(v: T): List<T> = indexOf(v).let {pos ->
    if (pos < 0) this else take(pos) + drop(pos + 1)
}

interface Expression {
    val value: Int
    val count: Int
    val numbers: List<Int>
    val parentheses: Int
    val priority: Priority
}

data class NumberExpression (val number: Int) : Expression {
    override val value = number
    override val count = 1
    override val numbers = listOf(number)
    override val parentheses = 0
    override val priority = Priority.ATOMIC
    override fun toString() = number.toString()
}

data class ArithmeticExpression (val leftOperand: Expression, 
        val operator: Operator, val rightOperand: Expression): Expression {
    override val value = operator.apply(leftOperand.value, rightOperand.value)
    override val count = leftOperand.count + rightOperand.count
    override val numbers = leftOperand.numbers + rightOperand.numbers
    val leftParenthesised = operator.priority > leftOperand.priority
    val rightParenthesised = operator.priority > rightOperand.priority ||
        (operator.priority == rightOperand.priority && !operator.commutative)
    override val parentheses = leftOperand.parentheses + rightOperand.parentheses +
        listOf(leftParenthesised, rightParenthesised).filter {it}.size
    override val priority = operator.priority
    fun parensIfNeeded(obj: Expression, parensNeeded: Boolean) =
        if (parensNeeded) "(" + obj.toString() + ")" else obj.toString()
    override fun toString() = listOf(parensIfNeeded(leftOperand, leftParenthesised),
        operator.symbol, parensIfNeeded(rightOperand, rightParenthesised)).joinToString(" ")
}

enum class Operator (val symbol: String, val priority: Priority, 
        val commutative: Boolean) {
    ADD("+", Priority.LOW, true) {
        override fun apply(a: Int, b: Int) = a + b
    },
    SUBTRACT("-", Priority.LOW, false) {
        override fun apply(a: Int, b: Int) = a - b
    },
    MULTIPLY("*", Priority.HIGH, true) {
        override fun apply(a: Int, b: Int) = a * b
    },
    DIVIDE("/", Priority.HIGH, false) {
        override fun apply(a: Int, b: Int) = a / b
    };
    abstract fun apply(a: Int, b: Int): Int
}

enum class Priority {
    LOW, HIGH, ATOMIC
}

typealias Combiner = (Expression) -> Expression?

enum class CombinerCreator {
    ADD {
        override fun combiner(leftOperand: Expression) = {rightOperand: Expression ->
            ArithmeticExpression(leftOperand, Operator.ADD, rightOperand)
        }
    },
    SUBTRACT {
        override fun combiner(leftOperand: Expression) = if (leftOperand.value < 3) null else
            {rightOperand: Expression -> 
                arrayOf(leftOperand.value, rightOperand.value).let {(lval, rval) ->
                    if (rval >= lval || rval * 2 == lval) null
                    else ArithmeticExpression(leftOperand, Operator.SUBTRACT, rightOperand)
                }    
            }
    },
    MULTIPLY {
        override fun combiner(leftOperand: Expression) = if (leftOperand.value == 1) null else
            {rightOperand: Expression -> 
                if (rightOperand.value == 1) null
                else ArithmeticExpression(leftOperand, Operator.MULTIPLY, rightOperand)
            }    
    },
    DIVIDE {
        override fun combiner(leftOperand: Expression) = if (leftOperand.value == 1) null else
            {rightOperand: Expression -> 
                arrayOf(leftOperand.value, rightOperand.value).let {(lval, rval) ->
                    if (lval % rval != 0 || rval * rval == lval) null
                    else ArithmeticExpression(leftOperand, Operator.DIVIDE, rightOperand)
                }    
            }
    };
    abstract fun combiner(leftOperand: Expression): Combiner?
}

fun combinersUsing(leftOperand: Expression): Sequence<Combiner> =
    CombinerCreator.values().asSequence().mapNotNull{it.combiner(leftOperand)}

typealias Difference = (Expression) -> Int

fun differenceFrom(target: Int): Difference = 
    {Math.abs(target - it.value)}

fun Difference.within(maxDiff: Int): (Expression) -> Boolean = 
    {this(it) <= maxDiff}

typealias Comparer = (Expression?, Expression) -> Expression

fun comparer(target: Int): Comparer =
    differenceFrom(target).let {diff ->
        {e1, e2 ->
            when {
                e1 == null -> e2
                diff(e1) < diff(e2) -> e1
                diff(e1) > diff(e2) -> e2
                e1.count < e2.count -> e1
                e1.count > e2.count -> e2
                e1.parentheses > e2.parentheses -> e2
                else -> e1
            }
        }
    }

suspend fun <T> Sequence<T>.parallelReduce(operation: (T, T) -> T, 
    chunkSize: Int = 1000, concurrency: Int = DEFAULT_CONCURRENCY): T = coroutineScope {
    chunked(chunkSize).asFlow()
        .flatMapMerge(concurrency) {
            flow { emit(it.reduce(operation)) }
        }
        .reduce(operation)
}
