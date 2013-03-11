
//Function returns the API gateway context path based on the base resource path
function getContextPath(baseResourcepath) {
	var returnValue="";
	for(var index in basePath_APIGWPathMap) {
		if(baseResourcepath == index)
		{
			returnValue=basePath_APIGWPathMap[index];
		}
	}
	 return returnValue;
 } 
 
 