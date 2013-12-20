/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
//import rssagregator.beans.DonneeBrute;
import rssagregator.beans.Flux;
import rssagregator.beans.Item;
//import rssagregator.beans.POJOCompteItem;
import rssagregator.beans.POJOCompteurFluxItem;
import rssagregator.beans.form.AbstrForm;
import rssagregator.beans.form.FORMFactory;
import rssagregator.beans.form.ItemForm;
import rssagregator.dao.DAOFactory;
import rssagregator.dao.DaoItem;
import rssagregator.dao.SearchFilter;
import rssagregator.utils.ServletTool;

/**
 * La servlet permettant de gérer l'acces des utilisateurs aux items. Elle est aussi utilisée dans le processus de
 * synchronisation. Cette servlet doit gérer les types d'action suivant :  
 * <ul>
 * <li><strong>read : </strong>l'utilisateur a demandé a lire les informations détaillées d'une items. </li>
 * <li><strong>rechercher : </strong> Permet de charger la page html permettant d'interroger les données items contenues
 * dans la base de données</li>
 * <li><strong>list : </strong>utilisé par l'interface ajax pour interroger la base de donnée et renvoyé des
 * informations sur les flux au format Json</li>
 * <li><strong>xmlsync :</strong> utilisée par le serveur maitre pour récupérer des données items sur le serveur
 * esclave. Les données sont envoyées au format XML</li>
 * </ul>
 *
 *
 * @author clem
 */
@WebServlet(name = "Item", urlPatterns = {"/item/*"})
public class ItemSrvl extends HttpServlet {

    public String VUE = null;
    public static final String ATT_ITEM = "item";
    public static final String ATT_SERV_NAME = "item";
    protected org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ItemSrvl.class);

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");


        String action = ServletTool.configAction(request, "recherche");

        //récupération de la vue
        String vue = request.getParameter("vue");
        if (vue == null) {
            vue = "html";
        }

        //DAO
        DaoItem daoItem = DAOFactory.getInstance().getDaoItem();
        daoItem.initcriteria();

//        ItemForm form = new ItemForm();

        Item item = null;


        System.out.println("ACTION : " + action);
        request.setAttribute("navmenu", "item");
        request.setAttribute("srlvtname", ATT_SERV_NAME);
        /**
         * **=================================================================================
         * ...................................ACTION READ
         *///*=================================================================================
        // L'action read permet de lire les informations détaillées sur une item. Elle a besoin de gérer les paramettres suivant :
        //          - id : Il s'agit de l'id de l'item à lire
        if (action.equals("read")) {
            String idString = request.getParameter("id");
            if (idString != null && !idString.isEmpty()) {
                Long id = new Long(idString);
                request.setAttribute("id", id);
                item = (Item) daoItem.find(id);
            }
            request.setAttribute(ATT_ITEM, item);
        }

        /**
         * *=================================================================================
         * ....................................ACTION LIST :
         *///=================================================================================
        //Il s'agit de l'action demandant en AJAX des informations sur les items. Elles seront renvoyées en JSON. 
//        if (action.equals("list")) {
//
//            /**
//             * Entrée des parametres pour compléter les vues
//             */
//            // On récupère le premier et dernier résult pour former des limites de requêtes.
//            Integer nbrItemPrPage;
//            try {
//                nbrItemPrPage = new Integer(request.getParameter("itPrPage"));
//            } catch (Exception e) {
//                nbrItemPrPage = 20;
//            }
//            request.setAttribute("itPrPage", nbrItemPrPage);
//            daoItem.setMaxResult(nbrItemPrPage);
//
//            //Récupération du firs result
//            Integer firsResult;
//            try {
//                firsResult = new Integer(request.getParameter("firstResult"));
//                daoItem.setFistResult(firsResult);
//                request.setAttribute("firstResult", firsResult);
//                System.out.println("FIRST result  : " + firsResult);
//
//            } catch (Exception e) {
//                firsResult = 0;
//                System.out.println("ERR first");
//            }
//            request.setAttribute("firsResult", firsResult);
//            daoItem.setFistResult(firsResult);
//
//
//
//            // SI on doit restreindre la sélection à un flux 
//            try {
//                String[] tabIdFluxString = request.getParameterValues("fluxSelection2");
//                List<Flux> listFluxEntites = new ArrayList<Flux>();
//
//                int i;
//                for (i = 0; i < tabIdFluxString.length; i++) {
//                    Flux f = (Flux) DAOFactory.getInstance().getDAOFlux().find(new Long(tabIdFluxString[i]));
//                    listFluxEntites.add(f);
//                }
//                daoItem.setWhere_clause_Flux(listFluxEntites);
//
//            } catch (Exception e) {
//                System.out.println("ERRRRRRRR" + e);
//                daoItem.setWhere_clause_Flux(null);
//            }
//
//            //Selection de l'ordre
//            try {
//                String s = request.getParameter("order");
//                String desc = request.getParameter("desc");
//                if (!s.isEmpty()) {
//                    if (s.equals("dateRecup") || s.equals("datePub") || s.equals("listFlux")) {
//                        daoItem.setOrder_by(s);
//                        if (desc.equals("true")) {
//                            daoItem.setOrder_desc(Boolean.TRUE);
//                        }
//                    }
//                }
//            } catch (Exception e) {
//            }
//
//            // Récupération des date limites
//            try {
//                String d1 = request.getParameter("date1");
//                DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy");
//                DateTime dateTime = fmt.parseDateTime(d1);
//                daoItem.setDate1(dateTime.toDate());
//            } catch (Exception e) {
//            }
//
//
//            try {
//                String d2 = request.getParameter("date2");
//                DateTimeFormatter fmt2 = DateTimeFormat.forPattern("dd/MM/yyyy");
//                DateTime dateTime2 = fmt2.parseDateTime(d2);
//                daoItem.setDate2(dateTime2.toDate());
//            } catch (Exception e) {
//            }
//
//
//            // Critère Sync Statut
//            try {
//                daoItem.setSynchStatut(new Integer(request.getParameter("syncStatut")));
//            } catch (Exception e) {
//            }
//
//            //On récupère le nombre max d'item
//            Integer nbItem = daoItem.findNbMax();
//            request.setAttribute("nbitem", nbItem);
//
//
//            //En fonction de la sélection demander on formule la bonne recherche
//            List<Item> listItem;
//
//            // si la vue est csv il faut enlever les limites
//            if (!vue.equals("html") ^ vue.equals("jsondesc")) { // en java le ^ est un XOR
//                daoItem.setFistResult(null);
//                daoItem.setMaxResult(null);
//            }
//
//            listItem = daoItem.findCretaria();
//            request.setAttribute("listItem", listItem);
//        }
        //--------------------------------------------------------------------------------------------------------------
        //.........................LIST GRID
        //--------------------------------------------------------------------------------------------------------------
        // Va être fusionné avec list
        if (action.equals("list")) {
            daoItem.initcriteria();

            // On récupère l'objet de gestion de formulaire
            AbstrForm formu = null;
            try {
                formu = FORMFactory.getInstance().getForm(Item.class, action);
                formu.parseListeRequete(request, daoItem); // On parse la requete avec l'objet formulaire afin de configurer la dao et des attributs dans la requete
            } catch (Exception ex) {
                Logger.getLogger(ItemSrvl.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (formu != null) {
                System.out.println("SERVLET NBR FILTER : " + formu.getFiltersList().getFilters().size());
                daoItem.setCriteriaSearchFilters(formu.getFiltersList());
                request.setAttribute("filtersList", formu.getFiltersList());
                
            }


            // On effectue la recherche


            ServletTool.actionLIST(request, Item.class, ATT_ITEM, daoItem);
        }

        /**
         * *=================================================================================
         * .............................ACTION RECHERCHER
         *///================================================================================
        // Action recherche, correspond à la demande de la page permettant de lister les flux. Il est nécessaire de fournir les paramettres permettant de construire les menus déroulant dans la JSP
        if (action.equals("recherche")) {
            // On récupère la liste des flux utile à la génération du menu déroulant
            request.setAttribute("listflux", DAOFactory.getInstance().getDAOFlux().findAllFlux(false));

            //List des journaux
//            request.setAttribute("listJournaux", DAOFactory.getInstance().getDaoJournal().findall());
            request.setAttribute("listJournaux", DAOFactory.getInstance().getDaoJournal().findallOrederByTitre());
        }

        /**
         * *=============================================================================
         * ...........................ACTION COMPTE
         *///=============================================================================
        if (action.equals("comptejour")) {

            // Récupération du filter
            String filterString = request.getParameter("filters");
            System.out.println("===========================");
            System.out.println(filterString);
            System.out.println("===========================");





            List<Item> listItem = null;
            Date date1 = null;
            Date date2 = null;
            List<Flux> listFlux = null;
            ItemForm form = null;
            try {
                form = (ItemForm) FORMFactory.getInstance().getForm(Item.class, "list");
                form.parseListeRequete(request, daoItem);
                
                System.out.println("FILTER FORM : "+form.getFiltersList());
                
                
                
                daoItem.setCriteriaSearchFilters(form.getFiltersList());

                // Il faut récupérer les deux date pour le trie
                List<SearchFilter> filtresList = form.getFiltersList().getFilters();
                System.out.println("NB FILTER : " + filtresList.size());
                for (int i = 0; i < filtresList.size(); i++) {
                    SearchFilter searchFilter = filtresList.get(i);
                    System.out.println("TYPE : " + searchFilter.getType());
                    if (searchFilter.getType().equals(Date.class)) {
                        if (date1 == null) {
                            date1 = (Date) searchFilter.getData();
                        } else if (date2 == null) {
                            date2 = (Date) searchFilter.getData();
                        }
                    }
                    
                    if(searchFilter.getField().equals("listFlux")){
                        
                        System.out.println(">>>>>>>>>>>>>>>ID FLUX ; " + searchFilter.getData());
                        System.out.println("CLASS " + searchFilter.getData().getClass());
                        if(searchFilter.getData().getClass().equals(JSONArray.class)){
                            System.out.println("ARRAY JSON");
                        }
                        listFlux = (List<Flux>) searchFilter.getData();
                    }
                }

              
                listItem = daoItem.findCriteria();
                for (int i = 0; i < listItem.size(); i++) {
                    Item searchFilter = listItem.get(i);
                    System.out.println("--> IT : " + searchFilter);
                }


            } catch (Exception ex) {
                Logger.getLogger(ItemSrvl.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("################################");
            System.out.println("---> date 1 " + date1);
            System.out.println("date 2 : " + date2);
            System.out.println("################################");

            System.out.println("--> LIST SIZE IT : " + listItem.size());

            POJOCompteurFluxItem compteurFluxItem = new POJOCompteurFluxItem();
            compteurFluxItem.setListItem(listItem);
            System.out.println("---> LIST ITEM : " + listItem.size());
            compteurFluxItem.setDate1(date1);
            compteurFluxItem.setDate2(date2);
            compteurFluxItem.setListFlux(listFlux);

            compteurFluxItem.compter();


            request.setAttribute("compte", compteurFluxItem.getListCompteItem());

        }
//        if(action.equals("donneesbrutes")){
//            
//            // récupération de l'id
//            List<Long> listID = ServletTool.parseidFromRequest(request, null);
//            for (int i = 0; i < listID.size(); i++) {
//                Long long1 = listID.get(i);
//                System.out.println("IDDD" + long1);
//            }
//
//            
//            
//            DAOGenerique dao = DAOFactory.getInstance().getDAOGenerique();
//            dao.setClassAssocie(DonneeBrute.class);
//            DonneeBrute brute =  (DonneeBrute) dao.find(listID.get(0));
//            
//               ObjectMapper mapper = new ObjectMapper();
//               
//                 FilterProvider filters = new SimpleFilterProvider().addFilter("serialisePourUtilisateur",
//                        SimpleBeanPropertyFilter.serializeAllExcept("flux, item"));
//                
//                String jsonn =  mapper.writer(filters).writeValueAsString(brute);
//                request.setAttribute("jsonstr", jsonn);
//                System.out.println(""+jsonn);
//                
//                vue = "jsonstr";
//                
//            
//            
//            
//            
//        }





        /**
         * *=================================================================================
         * .............................ACTION XML SYNC
         *///================================================================================
        // Il s'agit de l'action permettant au serveur maitre de récupérer des items sur le serveur esclave. Il effectue une requete en POST avec pour attribut : 
        //      - idflux : le flux pour lequel il veut des information
        //      - hash  : une longue chaine de caractère comprenant les hashs des items qu'il a pu capturer pour le flux en question. La servlet doit renvoyer les items ne possédant pas ces hash
        //      - date1 : critère de date pour la synchronisation. Ce critère s'applique sur la date de récupération
        //      - date2 : critère de date de fin
        if (action.equals("xmlsync")) {
            // Récupération des critères expliqué plus haut. On a déjà l'id
            String hash = request.getParameter("hash");
            String date1 = request.getParameter("date1");
            String date2 = request.getParameter("date2");
            String idflux = request.getParameter("idflux");


            Flux flux = (Flux) DAOFactory.getInstance().getDAOFlux().find(new Long(idflux));
            List<Flux> lFl = new ArrayList<Flux>();
            lFl.add(flux);

            // Configuration de la dao pour obtention des items concerné
            daoItem.setHashNotIn(hash);
            daoItem.setWhere_clause_Flux(lFl);

            List<Item> listItems = daoItem.findCretaria();
            request.setAttribute("listItem", listItems);

        }



        /**
         * *=================================================================================
         * ...............................GESTION DE LA VUE
         *///=================================================================================
        //utilisation de la vue en fonction des paramettres envoyé par l'utilisateur.
        if (vue.equals("html")) {
            VUE = "/WEB-INF/itemHTML.jsp";
        }
        if (vue.equals("csv")) {
            response.setHeader("Content-Disposition", "attachment; filename = items-export.csv");
            VUE = "/WEB-INF/itemCSV.jsp";
        } else if (vue.equals("csvexpert")) {
            response.setHeader("Content-Disposition", "attachment; filename = items-export.csv");
            VUE = "/WEB-INF/itemexpertCSV.jsp";
        } else if (vue.equals("jsondesc")) {
            System.out.println("ZOUZou");
            VUE = "/WEB-INF/itemJSONDesc.jsp";
        } else if (vue.equals("xls")) {
            response.setHeader("Content-Disposition", "attachment; filename = itemss-export.xls");
            VUE = "/WEB-INF/itemXLS.jsp";
        } else if (vue.equals("xmlsync")) {
            System.out.println("OUIIIII");
            VUE = "/WEB-INF/itemXMLsync.jsp";
        } else if (vue.equals("hightchart")) {
            VUE = "/WEB-INF/itemHighchart.jsp";
        } else if (vue.equals("grid")) {
            VUE = "/WEB-INF/itemJSONGrid.jsp";
        }
        else if(vue.equals("jsonstr")){
            VUE = "/WEB-INF/jsonPrint.jsp";
        }

        this.getServletContext().getRequestDispatcher(VUE).forward(request, response);
    }

    public static String getParam(String param, HttpServletRequest request) {

        String s = request.getParameter(param);
        if (s != null && !s.isEmpty()) {
            System.out.println("laa");
            return s;

        }
        return "";
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
