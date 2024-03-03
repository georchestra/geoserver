<html>
	<head>
		<style>
		 	body {
		   		margin:0;
		   		padding:0;
				background: #fff;
				color: var(--geoserver-primary);
				font-family: Tahoma, "Lucida Sans Unicode", "Lucida Grande", Verdana, sans-serif;
		 	}
		  	table {

			}
			th{
				background: transparent;
			}
			td{
				padding: 4px 10px 4px 5px;
			}
			thead th {
				background: var(--geoserver-secondary);
				border: 1px solid var(--geoserver-primary);
				border-width: 0 0 1px;
			}
			tr.even td,
			tr.even th {
				background: var(--geoserver-secondary-light);
			}
		</style>
	</head>
  	<body>
	  	<table width="100%">
	  		<thead>
	  			<th>Info</th>
	  			<th>Value</th>
	  		</thead>
		  	<#list properties.metrics as m>
		  	<tr class="${["odd", "even"][m_index%2]}">
		  		<td>${m.description}</td>
		  		<#assign x = m>
		  		<td>${m.valueUnit}</td>
		  	</tr>
		  	</#list>
		</table>
	</body>
 </html>