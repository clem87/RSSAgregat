<%@taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql"%>
<%-- 
    Document   : index
    Created on : 22 avr. 2013, 14:36:12
    Author     : clem
--%>
<%@page import="javax.el.ValueExpression"%>
<%@page import="rssagregator.servlet.FluxSrvl"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>  <!--Il faut bien utiliser la vesion 1.1 d ela jstl l'autre ne permet pas d'utiliser les EL-->
<%--<%@taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>--%>
<%--<%@page contentType="text/html" pageEncoding="UTF-8"%>--%>
<!--Inclusion du menu haut-->
<c:import url="/WEB-INF/headerjsp.jsp" />




<div id="header-wrapper">
    <div id="header">
        <div id="logo">
            <h1>Administration des <span>Flux</span></h1></div></div>


</div>

<div id="sidebar">
    <c:if test="${admin=='true'}"><p><a href="${rootpath}flux/add">Ajouter</a></p></c:if>
    
    <p><a href="${rootpath}flux/recherche">Recherche</a></p>
</div>




<div id="content">
    <div class="post">
        <c:choose >
            <c:when test="${not empty redirmap}">
                <p>${form.resultat}
                </p>
                <p>${redirmap['msg']}. </p>
                <c:if test="${err!='true'}">
                    Vous serez redirigé dans 3 secondes à l'adresse : <a href="${rootpath}${redirmap['url']}">${redirmap['url']}</a>
                    <script type="text/JavaScript">
                        <!--
                        setTimeout("location.href = '${rootpath}${redirmap['url']}';",3000);
                        -->
                    </script>
                </c:if>


            </c:when>
            <c:when test="${empty redirmap}">
                <c:choose>


                    <c:when test="${action=='recherche'}">
                        <form method="POST" id="pagina">
                            <input type="hidden" id="firstResult" value="0"/>

                            <fieldset>
                                <legend>Pages : </legend>
                                <%--<c:import url="/WEB-INF/paginator.jsp" />--%>
                                <div>
                                    <span id="btPaginDiv"></span>
                                </div>

                                <label>Flux par page</label>
                                <select id="itPrPage" name="itPrPage" onChange="$('#afin').click();"> 
                                    <c:forEach var="i" begin="10" end="150" step="20">
                                        <option value="${i}" <c:if test="${itPrPage==i}"> selected="selected"</c:if>>${i}</option>
                                    </c:forEach>
                                </select> 

                            </fieldset>

                            <fieldset>
                                <legend>Affiner la recherche</legend>
                                <label>Appartenant au journal : </label>
                                <select name="journalid" id="journalid">
                                    <option value="">TOUS</option>
                                    <c:forEach items="${listjournaux}" var="j">
                                        <option value="${j.ID}" <c:if test="${j.ID==journalid}"> selected="selected"</c:if>>${j.nom}</option>    
                                    </c:forEach>
                                </select>
                                <button type="button" id="afin">Affiner</button>
                                <!--<input type="submit" value="Affiner" onclick="$('#vue').val('')" id="sub" />-->

                                <select name="vue" id="vue" onchange="subExport()()">
                                    <option value="html">Exporter</option>
                                    <option value="opml">opml</option>
                                </select>
                                <script>

                                    function subExport(){
                                    if($('#vue').val()=='opml'){

                                    $('#pagina').attr('target', '_blank');
                                    $('#pagina').submit();
                                    $('#pagina').attr('target', '');
                                    $('#vue').val('html');


                                    }
                                    }
                                </script>
                                <button type="submit"  formaction="flux" formtarget="_blank" value="vue">Exporter</button>
                            </fieldset>
                        </form>
                        <script src="${rootpath}AjaxFluxDyn.js"></script>



                        <form id="formaction2">
                            <ul id="resudiv">

                            </ul>
                            <button type="button" value="0" id="bts">Tout sélectionner</button>
                            <select name="action" id="act">
                                <option value="rem">Supprimer</option>
                                <option value="maj">Mettre à jour</option>
                            </select>

                            <script>
                                $('#bts').click(function() {

                                if(this.value==='1'){
                                this.textContent='Déselectionner tout';
                                this.value='0';
                                $("#resudiv").find(':checkbox').prop('checked', false);
                                }
                                else if(this.value==='0'){
                                this.textContent='Tout sélectionner';
                                this.value='1';
                                $("#resudiv").find(':checkbox').prop('checked', true);
                                }
                                });
                            </script>
                            <button type="button" onclick="actionsub()"> OK</button>
                        </form>
                        <script>
                            //Petite fonction pour la soumission du formulaire permettant la mise à jour et la suppression en nombre
                            function actionsub() {
                            action = $('#act').val();

                            $('#formaction2').attr('action', '${rootpath}flux/'+action);
                            $('#formaction2').submit();
                            }

                        </script>
                    </c:when>
                    <c:when test="${action=='read-item' or action=='mod' or action=='read-incident'}">
                        <h1>Administration du flux : ${bean.url}</h1>
                        <ul>
                            <li><a href="${rootpath}item?id-flux=${bean.ID}">Parcourir les items du flux</a></li>
                            <li><a href="${rootpath}flux/mod?id=${bean.ID}">Configurer le flux</a></li>
                            <li><a href="${rootpath}flux/maj?id=${bean.ID}">Mettre à jour manuellement</a></li>
                            <li><a href="${rootpath}flux/rem?id=${bean.ID}">Supprimer le flux</a></li>
                            <li><a href="${rootpath}flux/read-incident&id=${bean.ID}">Parcourir les incidents</a></li>
                        </ul>
                    </c:when> 
                </c:choose>

                <c:choose> 
                    <c:when test="${action=='add' or action=='mod'}">
                        ${form.resultat}
                        <form method="POST" action="${rootpath}flux/${action}" id="formbean">

                            <input type="hidden" value="<c:out value="${action}"></c:out>" name="action">
                                <fieldset>
                                    <legend>Paramètres :</legend>

                                    <label for="active" title="L'agrégateur doit t'il collecter ce flux ?">Actif : <span class="requis"></span></label>
                                    <input type="checkbox" id="active" name="active" <c:if test="${bean.active=='true'}">checked="checked"</c:if>/>

                                    <br />
                                    <label for="url" title="Adresse a laquelle on trouve le XML du flux">URL du flux<span class="requis">*</span></label>
                                    <input type="text" id="url" name="url" value="<c:out value="${form.erreurs['url'][0]}" default="${bean.url}" />" size="20" maxlength="60" />
                                <span class="erreur"> ${form.erreurs['url'][1]}</span><br />

                                <label title="Indiquez la page html de la rubrique capturée">Page html</label>
                                <input name="htmlUrl" type="text" value="<c:out value="${form.erreurs['htmlUrl'][0]}" default="${bean.htmlUrl}" />"/>
                                <span class="erreur"> ${form.erreurs['htmlUrl'][1]}</span>
                                <br />

                                <label>Comportement de collecte : </label>
                                <select name="mediatorFlux">
                                    <c:forEach items="${listcomportement}" var="compo">
                                        <option value="${compo.ID}" <c:if test="${bean.mediatorFlux.ID==compo.ID}"> selected="true"</c:if> >${compo}</option>
                                    </c:forEach>
                                </select><br />

                                <label for="journalLie" title="Un journal comprends plusieurs flux... Si vous ne trouvez pas le journal concerné, allez dans Journal-> ajouter">Journal :</label>
                                <select name="journalLie" id="journalLie">
                                    <option value="-1">Aucun</option>
                                    <c:forEach items="${listjournaux}" var="journal">
                                        <option<c:if test="${journal.ID==bean.journalLie.ID}"> selected="selected"</c:if> value="${journal.ID}">${journal.nom}</option>
                                    </c:forEach>
                                </select>


                                <br />
                                <label title="La rubrique du journal concernée : international, A la Une ... Pour ajouter des types de flux allez dans la configuration générale">Type de flux</label>
                                <select name="typeFlux">
                                    <option value="-1">Aucun</option>
                                    <c:forEach items="${listtypeflux}" var="typeflux">
                                        <option<c:if test="${bean.typeFlux.denomination==typeflux.denomination}"> selected="true" </c:if> value="${typeflux.ID}">${typeflux.denomination}</option>
                                    </c:forEach>
                                </select>
                                <br />

                                <label for="nom" title="Paramettre facultatif : Par défaut le flux sera nommé en fonction du journal et du type de flux sélectionné. Ce paramettre permet de forcer un nom">Nom du flux : </label>
                                <input type="text" name="nom" value="${bean.nom}" />

                                <br />


                                <label for="parentFlux" title="Certain flux sont des sous classement d'autres. Exemple le flux Europe est un sous flux de International">Sous flux de : </label>
                                <select name="parentFlux" id="parentFlux">
                                    <option>NULL</option>
                                    <c:forEach items="${bean.journalLie.fluxLie}" var="fluxJournal">
                                        <option <c:if test="${fluxJournal.ID==flux.parentFlux.ID}"> selected="true"</c:if>>${fluxJournal}</option>
                                    </c:forEach>
                                </select>


                                <br />
                                <label for="infoCollecte">Information :</label><br />

                                <p>Flux créé le : <fmt:formatDate value="${bean.created}" pattern="dd/MM/yyyy hh:mm"/> </p>
                                <textarea id="infoCollecte" name="infoCollecte" rows="20" cols="80">${bean.infoCollecte}</textarea>

                                <input type="hidden" name="id" value="${bean.ID}">
                                <br />
                                <input type="submit" value="Enregitrer" class="sansLabel" />
                                <br />
                            </fieldset>
                        </form>

                        <p>Debug : Recap des levée</p>
                        <ul>
                            <c:forEach items="${bean.debug}" var="deb">
                                <li>${deb.date}    Nombre item : ${deb.nbrRecup} </li>
                            </c:forEach>
                        </ul>
                        <script src="${rootpath}test.js"></script>
                    </c:when>



                    <c:when test="${action=='read-incident'}">
                        <h2>Liste des incidets du flux</h2>
                        <c:forEach items="${bean.incidentsLie}" var="incid">

                            <li class="item">
                                <h3><a href="incidents?action=mod&id=${incid.ID}">${incid}</a></h3>
                                <p>Début : ${incid.dateDebut} fin : ${incid.dateFin}</p>
                                <p>${incid.messageEreur}</p>

                            </li>

                        </c:forEach>
                    </c:when>

                    <c:when test="${action=='maj'}">
                        <table border="1">
                            <tr>
                                <th>Flux</th>
                                <th>Item trouvée</th>
                                <th>Dedoub mémoire</th>
                                <th>Dedoub BDD</th>
                                <th>Item liée</th>
                                <th>Item nouvelle</th>
                                <th>Statut</th>
                            </tr>


                            * 1=nombre item trouvé ; 2 dedoub memoire; 3 BDD item lié ;4 BDD item déjà présente mais lien ajouté ;  5 item nouvelles

                            <c:forEach items="${listflux}" var="fl">
                                <tr>
                                    <td> ${fl}</td>
                                    <td>${fl.mediatorFluxAction.dedoubloneur.compteCapture[0]}</td>
                                    <td>${fl.mediatorFluxAction.dedoubloneur.compteCapture[1]}</td>
                                    <td>${fl.mediatorFluxAction.dedoubloneur.compteCapture[2]}</td>
                                    <td>${fl.mediatorFluxAction.dedoubloneur.compteCapture[3]}</td>
                                    <td>${fl.mediatorFluxAction.dedoubloneur.compteCapture[4]}</td>
                                    <td>
                                        <c:set var="erreur" value="0"></c:set>
                                        <c:forEach items="${fl.incidentEnCours}" var="inci" varStatus="last">
                                            <c:set var="erreur" value="1"></c:set>
                                            ${inci.messageEreur}
                                        </c:forEach>
                                        <c:if test="${erreur!=1}">OK</c:if>
                                    </tr>
                            </c:forEach>
                        </table>
                    </c:when>
                            
                    <c:when test="${action=='read'}">
                        <c:import url="/WEB-INF/inc/editionBean.jsp" />
                        
                       
                        <p><strong>Url : </strong> ${bean.url}</p>
                        <p><strong>Nom du flux :</strong> ${bean.nom}</p>
                        <p><strong>Page HTML :</strong> ${bean.htmlUrl}</p>
                        <p><strong>Comportement de collecte :</strong><a href="${rootpath}ComportementCollecte/read?id=${bean.mediatorFlux.ID}"> ${bean.mediatorFlux}</a></p>
                        <p><strong>Journal : </strong><a href="${rootpath}journaux/read?id=${bean.journalLie.ID}">${bean.journalLie}</a></p>
                        <p><strong>Type de flux :</strong><a href="${rootpath}TypeFluxSrvl/read?id=${bean.typeFlux.ID}"> ${bean.typeFlux}</a></p>
                        <p><strong>Ajouté le :</strong> ${bean.created}</p>
                        <p><strong>Période de captation : </strong><ul>
                            <c:forEach items="${bean.periodeCaptations}" var="periode">
                                <li>${periode}</li>   
                                
                            </c:forEach>
                        </ul>
                        
                        </p>
                        <hr />
                        <p><strong title="Blabla Explicatif">Indice captation : </strong>${bean.indiceQualiteCaptation}</p>
                        <p><strong>Mediane : </strong>${bean.indiceMedianeNbrItemJour}</p>
                        <p><strong>Décile :</strong>${bean.indiceDecileNbrItemJour}</p>
                        <p><strong>Quartile : </strong>${bean.indiceQuartileNbrItemJour}</p>
                        <p><strong>Maximum : </strong>${bean.indiceMaximumNbrItemJour}</p>
                        <p><strong>Minimum : </strong>${bean.indiceMinimumNbrItemJour}</p>
                        
                        
                        <script src="http://code.highcharts.com/highcharts.js"></script>
<script src="http://code.highcharts.com/highcharts-more.js"></script>
<script src="http://code.highcharts.com/modules/exporting.js"></script>
${bean.indiceQuartileNbrItemJour}ddd
<script>
    
    $(function () {
    $('#container').highcharts({

	    chart: {
	        type: 'boxplot'
	    },
	    
	    title: {
	        text: 'Highcharts Box Plot Example'
	    },
	    
	    legend: {
	        enabled: false
	    },
	
	    xAxis: {
	        categories: ['1', '2', '3', '4', '5'],
	        title: {
	            text: 'Experiment No.'
	        }
	    },
	    
	    yAxis: {
	        title: {
	            text: 'Observations'
	        },
	        plotLines: [{
	            value: 932,
	            color: 'red',
	            width: 1,
	            label: {
	                text: 'Theoretical mean: 932',
	                align: 'center',
	                style: {
	                    color: 'gray'
	                }
	            }
	        }]  
	    },
	
	    series: [{
	        name: 'Observations',
	        data: [
	            [${bean.indiceMinimumNbrItemJour}, ${bean.indiceQuartileNbrItemJour}, ${bean.indiceMedianeNbrItemJour}, ${bean.indiceDecileNbrItemJour}, ${bean.indiceMaximumNbrItemJour}]
	        ],
	        tooltip: {
	            headerFormat: '<em>Experiment No {point.key}</em><br/>'
	        }
	    }, {
	        name: 'Outlier',
	        color: Highcharts.getOptions().colors[0],
	        type: 'scatter',
	        data: [ // x, y positions where 0 is the first category
	            [0, 644],
	            [4, 718],
	            [4, 951],
	            [4, 969]
	        ],
	        marker: {
	            fillColor: 'white',
	            lineWidth: 1,
	            lineColor: Highcharts.getOptions().colors[0]
	        },
	        tooltip: {
	            pointFormat: 'Observation: {point.y}'
	        }
	    }]
	});
});
    
</script>


<div id="container" style="height: 400px; margin: auto; min-width: 310px; max-width: 600px"></div>



                    </c:when>
                </c:choose>
            </c:when>
        </c:choose>
    </div>
</div>

<c:import url="/WEB-INF/footerjsp.jsp" />