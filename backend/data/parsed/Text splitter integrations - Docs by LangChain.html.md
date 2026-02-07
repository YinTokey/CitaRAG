# Text splitter integrations - Docs by LangChain.html

We value your privacy
We use cookies to analyze our traffic. By clicking "Accept All", you consent to our use of cookies. Privacy Policy

Customize Reject All Accept All 




Customize Consent Preferences 
We may use cookies to help you navigate efficiently and perform certain functions. You will find detailed information about all cookies under each consent category below.
The cookies that are categorized as "Necessary" are stored on your browser as they are essential for enabling the basic functionalities of the site.... Show more



NecessaryAlways Active
Necessary cookies are required to enable the basic features of this site, such as providing secure log-in or adjusting your consent preferences. These cookies do not store any personally identifiable data.






Functional

Functional cookies help perform certain functionalities like sharing the content of the website on social media platforms, collecting feedback, and other third-party features.






Analytics

Analytical cookies are used to understand how visitors interact with the website. These cookies help provide information on metrics such as the number of visitors, bounce rate, traffic source, etc.






Performance
Performance cookies are used to understand and analyze the key performance indexes of the website which helps in delivering a better user experience for the visitors.






Advertisement

Advertisement cookies are used to provide visitors with customized advertisements based on the pages you visited previously and to analyze the effectiveness of the ad campaigns.






Uncategorized
Other uncategorized cookies are those that are being analyzed and have not been classified into a category as yet.







Reject All Save My Preferences Accept All






Skip to main content

Docs by LangChain home page
Open source
Search...

⌘K
	Ask AI
	GitHub
	Try LangSmith
	Try LangSmith




Search...


Navigation
Integrations by component
Text splitter integrations


Deep Agents
LangChain
LangGraph
Integrations
Learn
Reference
Contribute





Python



	LangChain integrations



	All providers





Popular Providers

	OpenAI


	Anthropic


	Google


	AWS


	Hugging Face


	Microsoft


	Ollama


	Groq






Integrations by component

	Chat models


	Tools and toolkits


	Middleware


	Retrievers


	Text splitters


	Embedding models


	Vector stores


	Document loaders


	Key-value stores







On this page	Text structure-based
	Length-based
	Document structure-based




Integrations by component

Text splitter integrations
Copy page



Integrate with text splitters using LangChain.

Copy page

pip

uv

Copy



pip install -U langchain-text-splitters









Text splitters break large docs into smaller chunks that will be retrievable individually and fit within model context window limit.
There are several strategies for splitting documents, each with its own advantages.

For most use cases, start with the RecursiveCharacterTextSplitter. It provides a solid balance between keeping context intact and managing chunk size. This default strategy works well out of the box, and you should only consider adjusting it if you need to fine-tune performance for your specific application.


​

Text structure-based

Text is naturally organized into hierarchical units such as paragraphs, sentences, and words. We can leverage this inherent structure to inform our splitting strategy, creating split that maintain natural language flow, maintain semantic coherence within split, and adapts to varying levels of text granularity. LangChain’s RecursiveCharacterTextSplitter implements this concept:

	The RecursiveCharacterTextSplitter attempts to keep larger units (e.g., paragraphs) intact.

	If a unit exceeds the chunk size, it moves to the next level (e.g., sentences).

	This process continues down to the word level if necessary.



Example usage:
Copy


from langchain_text_splitters import RecursiveCharacterTextSplitter

text_splitter = RecursiveCharacterTextSplitter(chunk_size=100, chunk_overlap=0)
texts = text_splitter.split_text(document)






Available text splitters:

	Recursively split text



​

Length-based

An intuitive strategy is to split documents based on their length. This simple yet effective approach ensures that each chunk doesn’t exceed a specified size limit. Key benefits of length-based splitting:

	Straightforward implementation

	Consistent chunk sizes

	Easily adaptable to different model requirements



Types of length-based splitting:

	Token-based: Splits text based on the number of tokens, which is useful when working with language models.

	Character-based: Splits text based on the number of characters, which can be more consistent across different types of text.



Example implementation using LangChain’s CharacterTextSplitter with token-based splitting:
Copy


from langchain_text_splitters import CharacterTextSplitter

text_splitter = CharacterTextSplitter.from_tiktoken_encoder(
    encoding_name="cl100k_base", chunk_size=100, chunk_overlap=0
)
texts = text_splitter.split_text(document)






Available text splitters:

	Split by tokens

	Split by characters



​

Document structure-based

Some documents have an inherent structure, such as HTML, Markdown, or JSON files. In these cases, it’s beneficial to split the document based on its structure, as it often naturally groups semantically related text. Key benefits of structure-based splitting:

	Preserves the logical organization of the document

	Maintains context within each chunk

	Can be more effective for downstream tasks like retrieval or summarization



Examples of structure-based splitting:

	Markdown: Split based on headers (e.g., #, ##, ###)

	HTML: Split using tags

	JSON: Split by object or array elements

	Code: Split by functions, classes, or logical blocks



Available text splitters:

	Split Markdown

	Split JSON

	Split code

	Split HTML






Edit this page on GitHub or file an issue.



Connect these docs to Claude, VSCode, and more via MCP for real-time answers.


Was this page helpful?
YesNo




Retriever integrations
Previous

Embedding model integrations
Next


⌘I






Docs by LangChain home pagegithubxlinkedinyoutube

Resources
ForumChangelogLangChain AcademyTrust Center

Company
HomeAboutCareersBlog


githubxlinkedinyoutube


Powered by







Assistant



Responses are generated using AI and may contain mistakes.