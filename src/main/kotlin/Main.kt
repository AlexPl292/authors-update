import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.kohsuke.github.GitHub
import java.io.File

fun main() {
    val repository = RepositoryBuilder().setGitDir(File("/Users/alex.plate/Develop/Work/ideavim/.git")).build()
    val git = Git(repository)
//    val emails = git.log().call().take(10).mapTo(HashSet()) { it.authorIdent.emailAddress }
    val emails = setOf("daya0576@gmail.com")

    val gitHub = GitHub.connect()
    val searchUsers = gitHub.searchUsers()
    val users = mutableListOf<Author>()
    for (email in emails) {
        if (email == "aleksei.plate@jetbrains.com") continue
        val user = searchUsers.q(email).list().single()
        val htmlUrl = user.htmlUrl.toString()
        val name = user.name
        users.add(Author(name, htmlUrl, email))
    }

    val authorsFile = File("/Users/alex.plate/Develop/Work/ideavim/AUTHORS.md")
    val authors = authorsFile.readText()
    val parser = MarkdownParser(GFMFlavourDescriptor())
    val tree = parser.buildMarkdownTreeFromString(authors)

    val contributorsSection = tree.children[24]
    val existingEmails = mutableSetOf<String>()
    for (child in contributorsSection.children) {
        if (child.children.size > 1) {
            existingEmails.add(child.children[1].children[0].children[2].children[2].getTextInNode(authors).toString())
        }
    }

    val newAuthors = users.filterNot { it.mail in existingEmails }

    val insertionString = newAuthors.toMdString()
    val resultingString = StringBuffer(authors).insert(contributorsSection.endOffset, insertionString).toString()

    authorsFile.writeText(resultingString)
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
