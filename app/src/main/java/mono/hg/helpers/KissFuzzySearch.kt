package mono.hg.helpers

import android.util.Pair
import java.util.*

/**
 * Fuzzy search class, code taken from works of saveman71 at KISS Launcher.
 * https://github.com/Neamar/KISS/commit/41aaec1e27da79fea7c929146cbababe3acac64e
 *
 *
 * Parts have been slightly altered to fit our codes here.
 */
object KissFuzzySearch {
    fun doFuzzy(sourceName: String?, queryMatch: String): Int {
        val matchPositions = ArrayList<Pair<Int, Int>>()
        var relevance = 0
        var queryPos = 0
        var appPos = 0
        var beginMatch = 0
        var matchedWordStarts = 0
        var totalWordStarts = 0
        var match = false

        // Normalise query and source (app name).
        val source = sourceName!!.toLowerCase(Locale.getDefault())
        val matchTo = queryMatch.toLowerCase(Locale.getDefault()).trim { it <= ' ' }
        for (cApp in source.toCharArray()) {
            if (queryPos < matchTo.length && matchTo[queryPos] == cApp) {
                // If we aren't already matching something, let's save the beginning of the match
                if (!match) {
                    beginMatch = appPos
                    match = true
                }

                // If we are at the beginning of a word, add it to matchedWordStarts
                if (appPos == 0 || Character.isWhitespace(source[appPos - 1])) {
                    matchedWordStarts += 1
                    totalWordStarts += 1
                }

                // Increment the position in the query
                queryPos++
            } else if (match) {
                matchPositions.add(Pair.create(beginMatch, appPos))
                match = false
            }
            appPos++
        }
        if (match) {
            matchPositions.add(Pair.create(beginMatch, appPos))
        }
        if (queryPos == matchTo.length) {
            // Add percentage of matched letters at a weight of 100
            relevance += (queryPos.toDouble() / source.length * 100).toInt()

            // Add percentage of matched upper case letters (start of word), but at a weight of 60
            relevance += (matchedWordStarts.toDouble() / totalWordStarts * 60).toInt()

            // The more fragmented the matches are, the less the result is important
            relevance = (relevance * (0.1 + 0.6 * (1.0 / matchPositions.size))).toInt()
        }
        return relevance
    }
}