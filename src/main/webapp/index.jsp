<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-type" content="text/html;charset=UTF-8">

    <title>REST Resources : O2 API Platform Documentation</title>
    <link href='http://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'/>
    <link href='css/hightlight.default.css' media='screen' rel='stylesheet' type='text/css'/>
    <link href='css/screen.css' media='screen' rel='stylesheet' type='text/css'/>
    <script src='lib/jquery-1.8.0.min.js' type='text/javascript'></script>
    <script src='lib/jquery.slideto.min.js' type='text/javascript'></script>
    <script src='lib/jquery.wiggle.min.js' type='text/javascript'></script>
    <script src='lib/jquery.ba-bbq.min.js' type='text/javascript'></script>
    <script src='lib/handlebars-1.0.rc.1.js' type='text/javascript'></script>
    <script src='lib/underscore-min.js' type='text/javascript'></script>
    <script src='lib/backbone-min.js' type='text/javascript'></script>
    <script src='lib/swagger.js' type='text/javascript'></script>
	<script src='lib/util.js' type='text/javascript'></script>
    <script src='swagger-ui.js' type='text/javascript'></script>
    <script src='lib/highlight.7.3.pack.js' type='text/javascript'></script>

    <style type="text/css">
        .swagger-ui-wrap {
            max-width: 960px;
            margin-left: auto;
            margin-right: auto;
        }

        .icon-btn {
            cursor: pointer;
        }

        #message-bar {
            min-height: 30px;
            text-align: center;
            padding-top: 10px;
        }

        .message-success {
            color: #89BF04;
        }

        .message-fail {
            color: #cc0000;
        }
    </style>
    <%
    String scheme = request.getScheme();             
    String serverName = request.getServerName(); 
    int serverPort = request.getServerPort();    
    String contextPath=request.getContextPath();
    String clientBasePathURL = scheme + "://" +serverName + ":" + serverPort+contextPath;
    %>
    
    

	<script type="text/javascript">
		var basePathURI='<%=clientBasePathURL%>';
		<!-- JS variable to be modified accordingly based on the environment-->
		<!-- domainResourcesEndPoint : A JS Array containing list of domain API  resources path-->
		<!-- basePath_APIGWPathMap : A JS map containing mapping resources lists, the key contains the API resources basepath and the value will be the API gateway resource path. During run time the API gateway resource path will be identified based on the resource base path.-->
		var domainResourcesEndPoint =['<%=clientBasePathURL%>/Customer.res?resourceName=customers','<%=clientBasePathURL%>/Tariff.res?resourceName=tariffs']
	    var basePath_APIGWPathMap = {'<%=clientBasePathURL%>/Customer.res':'<%=clientBasePathURL%>/Customer.api','<%=clientBasePathURL%>/Tariff.res':'<%=clientBasePathURL%>/Tariff.api'};

	</script>

   <script type="text/javascript">
   jQuery.extend({
		fetchAPIDetails : function (url,domid){
	 			window.swaggerUi = new SwaggerUi({
					discoveryUrl:url,
					apiKey:"special-key",
					dom_id:domid,
					supportHeaderParams: false,
					supportedSubmitMethods: ['get', 'post', 'put'],
					onComplete: function(swaggerApi, swaggerUi){
						if(console) {
							console.log("Loaded SwaggerUI")
							console.log(swaggerApi);
							console.log(swaggerUi);
						}
					  $('pre code').each(function(i, e) {hljs.highlightBlock(e)});
					},
					onFailure: function(data) {
						if(console) {
							console.log("Unable to Load SwaggerUI");
							console.log(data);
						}
					},
					docExpansion: "none"
				});

				window.swaggerUi.load();
        }
		});

    </script>
</head>

<body>
<div id='header'>
    <div class="swagger-ui-wrap">
        <a id="logo" href="#">O2 Platform -- API Docs</a>
    </div>
</div>

<div id="message-bar" class="swagger-ui-wrap">
    &nbsp;
</div>

<script>
	for(var index= 0; index < domainResourcesEndPoint.length; index++)
	{
		document.write("<div id=swagger-ui-container"+index+" class='swagger-ui-wrap'>");
		jQuery.fetchAPIDetails(domainResourcesEndPoint[index],'swagger-ui-container'+index);
		document.write("</div>");
    }
</script>
</body>
</html>
