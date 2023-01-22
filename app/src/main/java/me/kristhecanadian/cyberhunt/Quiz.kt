package me.kristhecanadian.cyberhunt

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class Quiz : AppCompatActivity() {

    companion object {
        private const val TAG = "Quiz"
        var quizNumber = 0
    }

    private var currentQuestion = 0
    private var score = 0
    private val questions = listOf(
        "What is spear phishing?",
        "What is a common method used in spear phishing attacks?",
        "What is the main difference between spear phishing and regular phishing?",
        "What is social engineering?",
        "How can you protect yourself from spear phishing attacks?"
    )
    private val answers = listOf(
        listOf("A type of phishing attack that uses a spear as bait", "A targeted phishing attack aimed at a specific individual or organization", "A phishing attack that is only successful on Fridays"),
        listOf("Placing a malicious link on a website", "Making phone calls to gather personal information", "Sending emails from a fake or spoofed email address"),
        listOf("Spear phishing is more targeted and personalized than regular phishing", "Spear phishing is less effective than regular phishing", "Spear phishing only uses text messages"),
        listOf("A type of phishing attack that only targets individuals", "The use of deception and manipulation to obtain sensitive information", "A type of malware that is difficult to detect"),
        listOf("Be suspicious of emails or messages that ask for personal information", "Be wary of clicking on links or downloading attachments from unknown sources", "Use anti-virus software and keep it up to date")
    )
    private val correctAnswers = listOf(1, 2, 0, 1, 0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cybersecurity_quiz)

        showNextQuestion()

    }

    private fun showNextQuestion() {
        if (currentQuestion < questions.size) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Question ${currentQuestion + 1}")
            builder.setMessage(questions[currentQuestion])

            val answerButtons = arrayOfNulls<Button>(answers[currentQuestion].size)
            val answerContainer = LinearLayout(this)
            answerContainer.orientation = LinearLayout.VERTICAL
            for (i in answers[currentQuestion].indices) {
                answerButtons[i] = Button(this)
                answerButtons[i]?.text = answers[currentQuestion][i]
                answerButtons[i]?.setOnClickListener {
                    checkAnswer(i)
                }
                answerContainer.addView(answerButtons[i])
            }
            builder.setView(answerContainer)

            builder.setCancelable(false)
            val dialog = builder.create()
            dialog.show()
        } else {
            showScore()
        }
    }

    private fun checkAnswer(answer: Int) {
        if (answer == correctAnswers[currentQuestion]) {
            score++
        }
        currentQuestion++
        showNextQuestion()
    }

    private fun writeScore() {
        // write score to local storage
        val sharedPref = getSharedPreferences("cyberhunt", Context.MODE_PRIVATE)
        Log.d(TAG, "Writing score $score to quiz$quizNumber")
        val editor = sharedPref.edit()
        editor.putInt("quiz$quizNumber", score)
        quizNumber++
        editor.apply()
    }

    private fun showScore() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quiz completed!")
        builder.setMessage("Your score is $score out of ${questions.size}")
        builder.setPositiveButton("OK") { _, _ ->
            writeScore()
            finish()
        }
        builder.setCancelable(false)
        builder.create().show()
    }


}