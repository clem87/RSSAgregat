/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.utils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import rssagregator.beans.AbstrObservableBeans;
import rssagregator.beans.FluxMBean;
import rssagregator.beans.Conf;
import rssagregator.beans.Flux;
import rssagregator.beans.form.AbstrForm;
import rssagregator.beans.form.FORMFactory;
import rssagregator.dao.AbstrDao;
import rssagregator.dao.DAOFactory;
import rssagregator.services.ServiceSynchro;

/**
 * Une série de methode static pouvant être utilisée dans les Servlet du projet.
 *
 * @author clem
 */
public class ServletTool {

    /**
     * *
     * Outil utilisé dans toutes les sevlet pour récupérer l'action demandé par l'utilisateur. La récupération se base
     * sur le path (request.getPathInfo).
     *
     * @param request la request dans la servlet
     * @param defaultAction L'action par défault. Devient la valeur de l'action si on ne trouve pas d'action dans la
     * request. Permet de rediriger sur recherche ou list facilement
     * @return l'action
     */
    public static String configAction(HttpServletRequest request, String defaultAction) {

        String action = request.getPathInfo();

        System.out.println("Path i" + action);
        if (action == null) {
            action = defaultAction;
        } else if (action.length() > 0) {
            action = action.substring(1, action.length());
        }
        request.setAttribute("action", action);
        return action;
    }

    /**
     * *
     * Une méthode pour gérer la redirmap
     *
     * @param request La request envoyée par la servlet
     * @param url adresse de redirection
     * @param msg message a afficher à l'utilisateur
     * @param err true si il s'agit d'une erreur. false si c'est une redirection de routine, l'utilisateur est alors
     * redirigé par javascript secondes après.
     */
    public static void redir(HttpServletRequest request, String url, String msg, Boolean err) {

        HashMap<String, String> redirmap = new HashMap<String, String>();
        redirmap.put("url", url);
        redirmap.put("msg", msg);
        request.setAttribute("redirmap", redirmap);
        if (err) {
            request.setAttribute("err", "true");
        }
    }

    /**
     * *
     * Cette méthode est utilisée par les servlet pour vérifier si un utilisateur a acces non à la page demandée. Elle
     * va s'intéresser a plusieurs paramettre : <ul>
     * <li>JMS : Si le serveur JMS n'est pas joignable on va refuser a l'utilisateur de lancer des action type add mod
     * del. En effet celles ci se serait pas répercuté sur les serveur esclave.</li>
     * <li>action l'action demandé par l'utilisateur Elle est récupéré a partir de l'url envoyé</li>
     * </ul>
     *
     * @param request : un tableau d'objet. La première case permet de savoir si l'acces est accepté ou non ; la seconde
     * contient une chaine de caractère avec le potentiel message d'erreur en cas de refus d'acces.
     * @return
     */
    public static boolean accesControl(HttpServletRequest request) {

        // On commence par récupérer l'action 
        String action = (String) request.getAttribute("action");
        Conf conf = DAOFactory.getInstance().getDAOConf().getConfCourante();

        //Pour les action de modification (add, mod del)
        if (action.equals("add") || action.equals("mod") || action.equals("del")) {
            //On vérifie le statut JMS
            Boolean statutJMS = ServiceSynchro.getInstance().getStatutConnection();
            // Si le serveur est maitre, qu'il possede des esclaves et que la connection JMS n'est pas active
            if (conf.getMaster() && conf.getServeurSlave().size() > 0 && !statutJMS) {
                request.setAttribute("accesmsg", "La connection n'est pas active. Votre action demande que la connection JMS soit active afin de répercuter les éventuelles moficications sur les serveurs esclaves");
                return false;
            }
            // Si c'est un serveur esclaves, on refuse des modification par les servlet. Les entitées ne doivent être rajouté que par synchronisation JMS
            if (!conf.getMaster()) {
                request.setAttribute("accesmsg", "Il s'agit d'un serveur esclave ! Vous ne devez pas ajouter d'entités sur un serveur esclave. Allez sur le serveur maitre pour faire vos modifications. Celles ci seront répercuté par la synchronisation");
                return false;
            }
        }
        return true;
    }

    /**
     * *
     * Récupère les id mentionnés dans la requête puis utilise la dao envoyé en paramettre pour rechercher les objets.
     * Retourne la liste des objets trouvés. Renvoi une exeption si un des id n'a pas pu être converti en Long ou si il
     * ne correspondait pas à un objet dans la base
     *
     * @param request
     * @param dao
     * @return
     * @throws NumberFormatException Exception levée si un des identifiants (id) trouvés dans la requête n'a pu être
     * convertis en Long
     * @throws NoResultException Exception levée si il n'y a pas d'objet dans la base de donnée pour un des identifiants
     * trouvés dans la requete
     */
    public static List getListFluxFromRequest(HttpServletRequest request, AbstrDao dao) throws NumberFormatException, NoResultException {
        List<Object> listFlux = new ArrayList<Object>();
        String[] tabIdf = request.getParameterValues("id");
        if (tabIdf != null && tabIdf.length > 0) {
            int i;
            for (i = 0; i < tabIdf.length; i++) {
                Long id = new Long(tabIdf[i]);

                Object o = dao.find(id);
                if (o == null) {
                    throw new NoResultException("Objet non trouvé dans la base de données");
                }
                listFlux.add((Flux) dao.find(id));
            }
        }
        return listFlux;
    }

    
    public static void mbeanEnregistre(Object o){
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            
            FluxMBean cast = (FluxMBean) o;
            ObjectName name = new ObjectName("rssagregator.beans:type=BeanIfs"+cast.getID());
            mbs.registerMBean(cast, name);
            
        } catch (MalformedObjectNameException ex) {
            Logger.getLogger(ServletTool.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstanceAlreadyExistsException ex) {
            Logger.getLogger(ServletTool.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MBeanRegistrationException ex) { 
            Logger.getLogger(ServletTool.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotCompliantMBeanException ex) {
            Logger.getLogger(ServletTool.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    
    /**
     * *
     * Permet de gérer l'action de read d'une servlet. Le paramettre id est recherche dans la request. La dao est
     * ensuite utilisée pour retrouver l'entité correpondante. Un attribue de la requete est ensuite associé au nom
     * beansnameJSP avec l'entitée récupéré par la dao
     *
     * @param request
     * @param dao
     * @param beansnameJSP
     */
    public static void actionREAD(HttpServletRequest request, Class beansClass, String beansnameJSP) {
        String srlvtname = (String) request.getAttribute("srlvtname");
        String id = request.getParameter("id");
        AbstrDao dao = DAOFactory.getInstance().getDaoFromType(beansClass);

        try {
            Object bean = dao.find(new Long(id));
            mbeanEnregistre(bean);
            
            if (bean == null) {
                throw new NoResultException();
            }
            request.setAttribute(beansnameJSP, bean);
        } catch (NoResultException e) {
            redir(request, srlvtname + "/read?id=" + id, "L'entité demandée n'existe pas !", Boolean.TRUE);
        } catch (NumberFormatException e) {
            redir(request, srlvtname + "/read?id=" + id, "L'entité demandée n'existe pas !", Boolean.TRUE);
        } catch (Exception e) {
            redir(request, srlvtname + "/read?id=" + id, "ERREUR lors du traitement : " + e, Boolean.TRUE);
        }
    }

    /**
     * *
     * Demande d'avoir au préalable configuré dans la servlet les attribus :
     * <ul>
     * <li><strong>srlvtname : une chaine de caractère contenanat le nom de la servlet</strong></li>
     * <li>form : L'objet de controle de formulaire</li>
     * </ul>
     *
     * @param request
     * @param dao
     * @param beansnameJSP
     * @param notifiObserver
     */
    public static void actionMOD(HttpServletRequest request, /*AbstrDao dao, */ String beansname, String formNameJSP, Class beansClass, Boolean notifiObserver) {
        String srlvtname = (String) request.getAttribute("srlvtname");
        String id = request.getParameter("id");
        AbstrForm f = null;
        AbstrDao dao = DAOFactory.getInstance().getDaoFromType(beansClass);
        try {
            Object bean = dao.find(new Long(id));
            request.setAttribute(beansname, bean);
            if (bean == null) {
                System.out.println("------------ NO RESULT");
                throw new NoResultException();
            }
            //On bind
            //On crée un formulaire 
            f = FORMFactory.getInstance().getForm(bean.getClass());
            System.out.println(" FORM DS TOOL : " + f);
            f.setAction("mod");
            request.setAttribute("form", f);

//            AbstrForm form = (AbstrForm) request.getAttribute("form");
            if (request.getMethod().equals("POST")) {
                //On tente de binder dans un objet nouveau
//                Object oTest = beansClass.newInstance();
                f.validate(request);

                // Si le bind a fonctionné, on bind pour de vrai.
                if (f.getValide()) {
                    bean = f.bind(request, bean, bean.getClass());


                    if (bean instanceof Flux) {
                        Flux ff = (Flux) bean;
                    }

                    dao.modifier(bean);

                    if (bean instanceof Flux) {
                        Flux ff = (Flux) bean;
                    }

                    if (notifiObserver && AbstrObservableBeans.class.isAssignableFrom(bean.getClass())) {
                        AbstrObservableBeans b = (AbstrObservableBeans) bean;
                        b.enregistrerAupresdesService();
                        b.forceChangeStatut();
                        b.notifyObservers();
                    }
                    //Si un type est précisé
                    String type = request.getParameter("type");
                    if (type == null) {
                        
                        redir(request, srlvtname + "/mod?id=" + id, "Traitement Effectué : ", Boolean.FALSE);
                    } else {
                        redir(request, srlvtname + "/mod?id=" + id + "&type=" + type, "Traitement Effectué : ", Boolean.FALSE);
                    }
                }
            }
            //Si le formulaire est valide, on effectue les modifications

        } catch (NumberFormatException e) {
            redir(request, srlvtname + "/mod?id=" + id, "L'entité demandée n'existe pas !", Boolean.TRUE);
            if (f != null) {
                f.setOperationOk(false);
                f.setResultat("Les données utilisateur sont valides, mais une erreur serveur est survenue : " + e);
            }
        } catch (NoResultException e) {
            redir(request, srlvtname + "/mod?id=" + id, "L'entité demandée n'existe pas !", Boolean.TRUE);
            if (f != null) {
                f.setOperationOk(false);
                f.setResultat("Les données utilisateur sont valides, mais une erreur serveur est survenue : " + e);
            }
        } catch (Exception e) {
            redir(request, srlvtname + "/mod?id=" + id, "ERREUR lors du traitement : " + e, Boolean.TRUE);
            if (f != null) {
                f.setOperationOk(false);
                f.setResultat("Les données utilisateur sont valides, mais une erreur serveur est survenue : " + e);
            }
        }
    }

    public static void actionADD(HttpServletRequest request, String beansnameJSP, String formNameJSP, Class beansClass, Boolean notifiObserver) {
        String srlvtname = (String) request.getAttribute("srlvtname");
        System.out.println("--->>>>>> ADD ");
        AbstrForm form = null;
        try {
            Object o = null;

            form = FORMFactory.getInstance().getForm(beansClass);
            form.setAction("add");
//            form.setAddAction(true);
            request.setAttribute(formNameJSP, form);
            AbstrDao dao = DAOFactory.getInstance().getDaoFromType(beansClass);

            if (request.getMethod().equals("POST")) {
                form.validate(request);
                request.setAttribute(beansnameJSP, o);
                if (form.getValide()) {
                    o = form.bind(request, o, beansClass);
                    dao.creer(o);
                    System.out.println("3");
                    if (notifiObserver && AbstrObservableBeans.class.isAssignableFrom(o.getClass())) {
                        AbstrObservableBeans aob = (AbstrObservableBeans) o;
                        aob.enregistrerAupresdesService();
                        aob.forceChangeStatut();
                        aob.notifyObservers("add");
                    }
                    redir(request, srlvtname + "/recherche", "AJOUT effectué : ", Boolean.FALSE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (form != null) {
//                form.setResultat("erreur lors de l'ajout : " + e);
                form.setOperationOk(false);
                form.setResultat("Les données utilisateur sont valides, mais une erreur serveur est survenue : " + e);
            }

            redir(request, srlvtname + "/add", "ERREUR lors du traitement : " + e, Boolean.TRUE);
        }
    }

    public static void actionADD2(HttpServletRequest request, String beansnameJSP, String formNameJSP, Class beansClass, Boolean notifiObserver) {
        String srlvtname = (String) request.getAttribute("srlvtname");
        System.out.println("--->>>>>> ADD ");
        try {
            Object o = null;

            AbstrForm form = FORMFactory.getInstance().getForm(beansClass);
            form.setAction("add");
//            form.setAddAction(true);
            request.setAttribute(formNameJSP, form);
            AbstrDao dao = DAOFactory.getInstance().getDaoFromType(beansClass);
            if (request.getMethod().equals("POST")) {
                form.validate(request);
                request.setAttribute(beansnameJSP, o);

                if (form.getValide()) {
                    o = form.bind(request, o, beansClass);
                    dao.creer(o);
                    if (notifiObserver && AbstrObservableBeans.class.isAssignableFrom(o.getClass())) {
                        AbstrObservableBeans aob = (AbstrObservableBeans) o;
                        aob.enregistrerAupresdesService();
                        aob.forceChangeStatut();
                        aob.notifyObservers("add");
                    }
                    redir(request, srlvtname + "/recherche", "AJOUT effectué : ", Boolean.FALSE);
                } else { // Si le formulaire n'est pas valide
                    System.out.println("Servlet : pas valid ");
                }
            }

        } catch (Exception e) {
            redir(request, srlvtname + "/add", "ERREUR lors du traitement : " + e, Boolean.TRUE);
            System.out.println("ERR : " + e);
        }
    }

    public static void actionREM(HttpServletRequest request, Class beansClass, Boolean notifiObserver) {
        String srlvtname = (String) request.getAttribute("srlvtname");
        AbstrDao dao = DAOFactory.getInstance().getDaoFromType(beansClass);
        String id;

        // On doit récupérer le beans
        try {
            Object o = dao.find(new Long(request.getParameter("id")));
            //On tente la suppression

            dao.remove(o);
            if (notifiObserver && beansClass.isAssignableFrom(AbstrObservableBeans.class)) {
                AbstrObservableBeans aob = (AbstrObservableBeans) o;
                aob.enregistrerAupresdesService();
                aob.forceChangeStatut();
                aob.notifyObservers("rem");
            }
            redir(request, srlvtname + "/recherche", "Suppression éffectué ! : ", Boolean.FALSE);

        } catch (ArithmeticException e) {
            redir(request, srlvtname + "/rem", "L'entitée demandée n'existe pas ! : ", Boolean.TRUE);
        } catch (NoResultException e) {
            redir(request, srlvtname + "/rem", "L'entitée demandée n'existe pas ! : ", Boolean.TRUE);
        } catch (Exception e) {
            redir(request, srlvtname + "/rem", "Erreur lors du traitement  : " + e, Boolean.TRUE);
        }

    }

    private ServletTool() {
    }
}
