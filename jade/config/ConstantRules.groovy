class RulesEngine {

	def evals = [:]
	def sigmappings = [:]
	def descmappings = [:]

	// Separate the map into the rules and the query evaluations:

	public rules ( Map rules ) {
		this.evals = rules;
		rules.each { rulename, deflist ->
			this.sigmappings.put(deflist[0], deflist[1])
			this.descmappings.put(deflist[0], deflist[3])
		}
	}

	// 给定(sig,value)，回答是否有问题
	Object evaluate ( String sig, String value) {
		return evals.any{ name, deflist ->
			return deflist[0] == sig && deflist[2].call(value)
		}
	}

	Map<String,Integer> getMappings()
	{
		this.sigmappings
	}

	Map<String,String> getDescMappings()
	{
		this.descmappings
	}

	// Given: CamelCased this returns camelCased
	String unTitleCase ( String str ) {
		str[0].toLowerCase() + str[1..str.size()-1 ]
	}
}
//ConstantBase(funcSig:"aaa", argIdx:0, vulnList=[1,2,3])

static RulesEngine getEngine(){
	def engine = new RulesEngine()

	engine.rules( [
			//rule, defn
			https:
					[
							"<org.apache.http.conn.ssl.SSLSocketFactory: void setHostnameVerifier(org.apache.http.conn.ssl.X509HostnameVerifier)>",
							0,
							{
								it == "<org.apache.http.conn.ssl.SSLSocketFactory: org.apache.http.conn.ssl.X509HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER>"
							},
							"SSL setHostnameVerifier"
					],
			httpsdefault:
					[
							"<javax.net.ssl.HttpsURLConnection: void setDefaultHostnameVerifier(javax.net.ssl.HostnameVerifier)>",
							0,
							{
								it == "<org.apache.http.conn.ssl.SSLSocketFactory: org.apache.http.conn.ssl.X509HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER>"
							},
							"SSL setHostnameVerifier"
					],
			cipher:
					[
							"<javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String)>",
							0,
							{
								it ==~ /"RSA(\/\w+\/None)?"/
							},
							"RSA crypto insecure"
					],
			getsharedpref:
					[
							"<android.content.ContextWrapper: android.content.SharedPreferences getSharedPreferences(java.lang.String,int)>",
							1,
							{
								it == "1" || it == "2"
							},
							"sharedprefs world mode"
					],
			openFileOutput:
					[
							"<android.content.ContextWrapper: java.io.FileOutputStream openFileOutput(java.lang.String,int)>",
							1,
							{
								it == "1" || it == "2"
							},
							"openfileoutput world mode"
					],
			jsinterface:
					[
							"<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)>",
							1,
							{
								true
							},
							"webview addjsinterface code exec"
					],
			parseUri:
					[
							"<android.content.Intent: android.content.Intent parseUri(java.lang.String,int)>",
							1,
							{
								true
							},
							"intent parseUri"
					],
			//admission: [
			//	  isBaby:    { 0.00 },
			//	  isChild:   { 3.00 },
			//	  isStudent: { 7.00 },
			//	  isSenior:  { 7.00 },
			//	  isAdult:   { 9.00 }
			//  ]
	] );
	return engine;
}

engine = getEngine();

Map<String,Integer> getMappings()
{
	engine.getMappings()
}

Map<String,String> getDescMappings()
{
	engine.getDescmappings()
}

boolean evaluate(String sig, String value)
{
	engine.evaluate(sig, value)
}
