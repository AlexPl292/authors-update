package dev.feedforward.authorsupdate

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.kohsuke.github.GitHub
import java.io.File

class UpdateAuthors {
    fun update(path: String, token: String, uncheckedEmails: Set<String>) {
        val repository = RepositoryBuilder().setGitDir(File("$path/.git")).build()
        val git = Git(repository)
        val emails = git.log().call().take(20).mapTo(HashSet()) { it.authorIdent.emailAddress }

        val gitHub = GitHub.connect()
        val searchUsers = gitHub.searchUsers()
        val users = mutableListOf<Author>()
        for (email in emails) {
            if (email in uncheckedEmails) continue
            val githubUsers = searchUsers.q(email).list().toList()
            if (githubUsers.isEmpty()) error("Cannot find user $email")
            val user = githubUsers.single()
            val htmlUrl = user.htmlUrl.toString()
            val name = user.name
            users.add(Author(name, htmlUrl, email))
        }

        val authorsFile = File("$path/AUTHORS.md")
        val authors = authorsFile.readText()
        val parser = MarkdownParser(GFMFlavourDescriptor())
        val tree = parser.buildMarkdownTreeFromString(authors)

        val contributorsSection = tree.children[24]
        val existingEmails = mutableSetOf<String>()
        for (child in contributorsSection.children) {
            if (child.children.size > 1) {
                existingEmails.add(
                    child.children[1].children[0].children[2].children[2].getTextInNode(authors).toString()
                )
            }
        }

        val newAuthors = users.filterNot { it.mail in existingEmails }
        if (newAuthors.isEmpty()) return

        val insertionString = newAuthors.toMdString()
        val resultingString = StringBuffer(authors).insert(contributorsSection.endOffset, insertionString).toString()

        authorsFile.writeText(resultingString)

        git.add().addFilepattern(".").call()
        val allNewAuthors = newAuthors.joinToString(", ") { it.name }

        println("Creating commit...")
        val commit = git.commit()
        commit.setSign(false)
        commit.setAuthor("Alex Plate", "aleksei.plate@jetbrains.com")
        commit.message = "Add $allNewAuthors to contributors list"
        commit.call()
        println("Created")

        println("Pushing...")
        val push = git.push()
        push.remote = "git@github.com:JetBrains/ideavim.git"
        push.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
        push.call()
        println("Pushed")
    }
}

fun List<Author>.toMdString(): String {
    return this.joinToString() {
        """
          |
          |* [![icon][mail]](mailto:${it.mail})
          |  [![icon][github]](${it.url})
          |  &nbsp;
          |  ${it.name}
        """.trimMargin()
    }
}

data class Author(
    val name: String,
    val url: String,
    val mail: String,
)
