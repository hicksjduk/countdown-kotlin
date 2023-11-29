# countdown-kotlin

A solver,
written in Kotlin, for the numbers game as played on [the TV show "Countdown"](https://www.channel4.com/programmes/countdown).

## The game

A Countdown numbers game involves building an expression using some source numbers (usually six of them), and the basic arithmetic
operations (addition, subtraction, multiplication and integer division), plus
as many pairs of parentheses as are needed to achieve the desired 
order of evaluation. The objective is to come up with an expression
whose value is as close as possible to the target, which is
a three-digit number in the range 100 to 999 inclusive. It is not necessary
to use all the source numbers, but no number can be used more times than
it occurs among the source numbers.

The source numbers are selected at random from a pool of twenty "small numbers" -
two instances each of the numbers 1 to 10 inclusive - and four "big numbers" -
one instance each of 25, 50, 75 and 100. 

The game can be played by any number of players, although on the standard version of the show there are only two. The procedure for a round of the numbers game is:
* One player specifies how many big numbers should
be selected (between 0 and 4 inclusive).
* Six source numbers are selected, including the specified number of big numbers.
* A target number is randomly generated.
* The players have thirty seconds to come up
with the best answer they can.
* Each player in turn declares the value of the answer they wish to submit.
* Starting with the player(s) whose declaration
is closest to the target, the players' answers are revealed. In order for a player's answer to be valid, their declared value must differ from the target number by not more than ten, and their answer must be a valid expression whose value is equal to their declared value.
* The player or players who submit valid answers which are closest to the target number score points. 10 points are scored for an
answer whose value is exactly equal to the target; 7 for one that differs from
the target by between 1 and 5 inclusive; and 5 for one that differs from the target by between
6 and 10 inclusive. If no player submits a valid answer, nobody scores any points.

## The solver

This solver attempts to find the best possible solution for the given target and
source numbers. What constitutes the best solution employs additional criteria
besides the one used on the show (closer to the target number):

* If two solutions differ from the target number by the same amount, one is considered
be better if it uses fewer source numbers.
* If two solutions differ from the target number by the same amount and use the same
number of source numbers, one is considered to be better if it requires fewer pairs
of parentheses.

If no expression can be made from the source numbers whose value differs from the
target number by ten or less, no solution is found.

## Algorithm structure

The algorithm for solving a Countdown numbers game has the following main steps:

* Make all possible unique permutations of the source numbers, including partial
permutations where not all the numbers are used.
* For each permutation, make all the expressions that can be used by combining the
numbers in the permutation, in the specified order, with the permitted arithmetic operators,
and parentheses as needed to vary the order of evaluation.
* From all the expressions made using all the permutations, find the best solution, if
any solutions exist.

The root of the solver algorithm is the `solve` function, which takes two parameters: an `Int` specifying the target number and a `vararg` of `Int` specifying the source numbers. It returns a value of type `Expression?`, which is `null` to denote that no solution could be found, or an `Expression` 
representing the best possible solution.