<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Music Radar</title>
	<link rel="stylesheet" type="text/css" href="/css2/music.css"/>
</head>
<body>
	<div id="dhPage">
		<div id="dhPageHeader">
			<div id="dhHeaderLogo"><img src="/images2/mugshot_logo.gif"/></div>
			<div id="dhSearchBox">
				Search: <input type="text" class="dh-text-input"/> <input type="button" value="Go"/>
			</div>
		</div>
		<div id="dhPageContent">
			<div id="dhSidebarColumn">
				<div class="dh-sidebar-box dh-profile-box">
					<div class="dh-title">
					MY PROFILE
					</div>
					<div class="dh-item">
						<table cellpadding="0" cellspacing="0">
							<tbody>
								<tr valign="top">
									<td>
										<div class="dh-image">
										</div>
									</td>
									<td>
										<div class="dh-next-to-image">
											<div class="dh-name">Monkey Mouth</div>
											<div class="dh-action-link"><a href="/account">Edit account</a></div>
											<div class="dh-action-link"><a href="">Sign out</a></div>
										</div>
									</td>
								</tr>
							</tbody>
						</table>
						<div class="dh-bio">
							I am so special. This is my biography. Please read more about me!
						</div>
					</div>
				</div>
				<div class="dh-sidebar-box dh-controls-box">
					<div class="dh-title">
					MUSIC RADAR CONTROLS
					</div>
					<div>
						Music embed: <input type="radio"/> On <input type="radio"/> Off
					</div>
					<div style="margin-top: 3px;"><a href="">Edit theme</a></div>
					<div class="dh-separator"><div></div></div>
					<div><a href="">Edit my music bio</a></div>
				</div>				
				<div class="dh-sidebar-box dh-groups-box">
					<div class="dh-title">
					MY GROUPS
					</div>
					<div class="dh-item">
						<table cellpadding="0" cellspacing="0">
							<tbody>
								<tr valign="top">
									<td>
										<div class="dh-image">
										</div>
									</td>
									<td>
										<div class="dh-next-to-image">
											<div class="dh-name"><a href="">Sky Devils</a></div>
											<div class="dh-info">7 members</div>
											<div class="dh-info">12 posts</div>
										</div>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div class="dh-item">
						<table cellpadding="0" cellspacing="0">
							<tbody>
								<tr valign="top">
									<td>
										<div class="dh-image">
										</div>
									</td>
									<td>
										<div class="dh-next-to-image">
											<div class="dh-name"><a href="">Happy Onions</a></div>
											<div class="dh-info">17 members</div>
											<div class="dh-info">2 posts</div>
										</div>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div class="dh-more"><a href="">MORE</a></div>
				</div>
			</div>
			<div id="dhContentColumn">
				<div class="dh-zone-box dh-color-music">
					<div class="dh-zone-box-header"><img src="/images2/header_music500.gif"/><div class="dh-zone-box-header-links">Jump to: <a href="">Link Swarm</a> | <a href="">TV Party</a></div></div>
					<div class="dh-zone-box-border">
						<div class="dh-zone-box-content dh-color-normal">					
							<div class="dh-title dh-color-music-foreground">CURRENTLY LISTENING TO</div>
							
							<div class="dh-nowplaying"><div></div></div>

							<div class="dh-subcolumns">
	
								<div class="dh-subcolumn dh-subcolumn-one">
		
									<div class="dh-title dh-color-music-foreground">SHARED WITH ME</div>
		
									<div class="dh-item">
										<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
										<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
										yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
										</div>
									</div>
									<div class="dh-item">
										<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
										<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
										focus on the sudden kitten shortage currently gripping the country.
										</div>
									</div>
									
									<div class="dh-more"><a href="">MORE</a> <img src="/images2/arrow_right.gif"/></div>

								</div>
								<div class="dh-subcolumn dh-subcolumn-two">
									
									<div class="dh-title dh-color-music-foreground">SHARED BY ME</div>
									<div class="dh-item">
										<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
										<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
										yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
										</div>
									</div>
									<div class="dh-item">
										<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
										<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
										focus on the sudden kitten shortage currently gripping the country.
										</div>
									</div>
									<div class="dh-item">
										<div class="dh-title"><a href="">HTML div needs to be longer</a></div>
										<div class="dh-blurb">Another item was needed in an HTML div to create unequal-length columns.
										</div>
									</div>									
									
									<div class="dh-more"><a href="">MORE</a> <img src="/images2/arrow_right.gif"/></div>

								</div>
							
								<div class="dh-grow-div-around-floats"><div></div></div>
							</div>

						</div>
					</div>
					<div><img src="/images2/bottom_music500.gif"/></div>					
				</div>
			</div>
		</div>
		<dht:footer/>
	</div>
</body>
</html>
