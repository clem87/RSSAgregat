/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;
import org.joda.time.DateTime;
import rssagregator.beans.Flux;
import rssagregator.beans.Item;
import rssagregator.beans.Journal;
import rssagregator.beans.exception.DonneeInterneCoherente;
import rssagregator.beans.exception.IncompleteBeanExeption;
import rssagregator.beans.exception.UnIncidableException;
import rssagregator.dao.CacheHashFlux;
import rssagregator.dao.DAOFactory;
import rssagregator.dao.DaoFlux;
import rssagregator.dao.DaoItem;
import rssagregator.dao.DaoJournal;

/**
 * Cette classe permet d'instancier le service de collecte du projet. Elle est organisée autours de deux objets
 * priomordiaux : le pool de tache schedulé qui permet de lancer périodiquement les tache lié aux flux ; et le pool de
 * tache manuelle qui permet annectodiquement de lancer la mise à jour des flux
 *
 * @author clem
 */
public class ServiceCollecteur extends AbstrService {

    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ServiceCollecteur.class);
//    ListeFluxCollecte fluxCollecte; On le récupère maintenant directement depuis le singleton de collecte
    /**
     * *
     * Instance du singleton récupérable par {@link #getInstance() }
     */
    private static ServiceCollecteur instance = new ServiceCollecteur();
    /**
     * *
     * Le cache du service de collecte parmettant de dédoublonner la majeur partie des items sans faire appel à la base
     * de données
     */
    private CacheHashFlux cacheHashFlux = CacheHashFlux.getInstance();
    /**
     * *
     * Cette map permet au service de retrouver les les tâche lié au flux. la clé de la map est l'id du flux (un
     * {@link Long}) la valeur est la tache récoltant le flux. C'est au service de maintenir cette map
     */
    private Map<Long, List<AbstrTacheSchedule>> mapFluxTache = new HashMap<Long, List<AbstrTacheSchedule>>();
    private Map<Long, TacheDecouverteAjoutFlux> mapJournalTache = new HashMap<Long, TacheDecouverteAjoutFlux>();

    /**
     * *
     * Constructeur du singleton
     */
    private ServiceCollecteur() {
        super();
        try {
            ThreadFactoryPrioitaire factoryPrioitaire = new ThreadFactoryPrioitaire();
            // Le nombre de thread doit être relevé dans la conf. 
            poolPrioritaire = Executors.newFixedThreadPool(5);

        } catch (ArithmeticException e) {
            logger.error("Impossible de charger le nombre de Thread pour ce service. Vérifier la conf", e);
        } catch (Exception e) {
            logger.error("Erreur lors de l'instanciation du service", e);
        }
    }

    /**
     * *
     * Ce service est in singleton.
     *
     * @return
     */
    public static ServiceCollecteur getInstance() {
        if (ServiceCollecteur.instance == null) {
            ServiceCollecteur.instance = new ServiceCollecteur();
        }
        return ServiceCollecteur.instance;
    }
    /**
     * *
     * Le pool de thread permettant de lancer des récupération de flux en passant devant le pool schedulé
     */
    private ExecutorService poolPrioritaire;

    /**
     * *
     * Tâche permettant de lancer la collecte d'un nouveau flux. Si le flux était déjà enregistré. Sa tache est détruite
     * et recrée
     *
     * @param f : le flux qu'il faut enregistrer auprès du collecteur
     */
    public synchronized void enregistrerFluxAupresDuService(Flux f) throws IncompleteBeanExeption {
        if (f == null) {
            throw new NullPointerException("le flux est null");
        }
        if (f.getID() == null) {
            throw new IncompleteBeanExeption("Il n'est pas possible d'enregistrer un flux à L'ID NULL");
        }

        if (f.getMediatorFlux() == null) {
            throw new IncompleteBeanExeption("Il n'est pas possible d'ajouter un flux ne possédant pas de Comportement de Collecte");
        }

        if (f.getMediatorFlux().getPeriodiciteCollecte() == null) {
            throw new IncompleteBeanExeption("Le comportement de collecte du flux ne permet pas de savoir la période de schedulation");
        }


        if (f.getActive()) {
            //On regarde si le flux est déjà enregistré dans la map. Il faut le supprimer si il est trouvé. On replacera ensuite de nouvelles taches

            List<AbstrTacheSchedule> listTache = mapFluxTache.get(f.getID());
            if (listTache != null) {
                for (int i = 0; i < listTache.size(); i++) {
                    AbstrTacheSchedule abstrTacheSchedule = listTache.get(i);
                    if (abstrTacheSchedule != null) {
                        abstrTacheSchedule.setAnnuler(true);
                        try {
                            abstrTacheSchedule.call();
                        } catch (Exception ex) {
                            Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }


            TacheRecupCallable tache = new TacheRecupCallable(f, this, Boolean.TRUE); // création de la nouvelle tache de collecte
            tache.setTimeSchedule(f.getMediatorFlux().getPeriodiciteCollecte());  // On définit son temsp de schedulation en fonction des paramettre de son comportement de collecte
            TacheCalculQualiteFlux tachecalcul = new TacheCalculQualiteFlux(this);// Création de la nouvelle tache de vérification.
            tachecalcul.setFlux(f);
            tachecalcul.setTimeSchedule(f.getMediatorFlux().getPeriodiciteCollecte() * 5); // Le calcul de la qualité est effectuer tous les 5* temps de récupération. 


            List<AbstrTacheSchedule> nouvList = new ArrayList<AbstrTacheSchedule>();
            nouvList.add(tache);

            nouvList.add(tachecalcul);


            // On ajoute la tache à la map
            mapFluxTache.put(f.getID(), nouvList);

            // On lance la tâche
//            this.executorService.schedule(tache, f.getMediatorFlux().getPeriodiciteCollecte(), TimeUnit.SECONDS);
            schedule(tache);
            schedule(tachecalcul);


        }
    }

    public synchronized void enregistrerJournalAupresduService(Journal j) throws IncompleteBeanExeption {
        if (j == null) {
            throw new NullPointerException("Le journal est null");
        }

        if (j.getID() == null) {
            throw new IncompleteBeanExeption("Le journal n'a pas d'ID");
        }

        // .....

        if (j.getAutoUpdateFlux()) {

            TacheDecouverteAjoutFlux tache = mapJournalTache.get(j.getID());

            if (tache != null) { // Si le service a déjà une tache pour le journal, elle doit être annulée
                try {
                    tache.annuler();
                } catch (Exception ex) {
                    Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            tache = new TacheDecouverteAjoutFlux();
            tache.setJournal(j);
            tache.setPersist(true);
            tache.setActiverLesFLux(j.getActiverFluxDecouvert());
            tache.setNombredeSousTache(30); // TODO : doit être tiré du beans Journal
            mapJournalTache.put(j.getID(), tache);

            this.executorService.schedule(tache, 60, TimeUnit.SECONDS); // TODO : devra être tiré du Journal ou mieux d'une entitée modélisant le comportement
        }
    }

    public synchronized void retirerJournalDuService(Journal j) throws IncompleteBeanExeption {
        if (j == null) {
            throw new NullPointerException("Impossible de désactiver un journal null");
        }
        if (j.getID() == null) {
            throw new IncompleteBeanExeption("Le journal n'a pas d'ID");
        }

        // On annule l atache
        TacheDecouverteAjoutFlux tache = mapJournalTache.get(j.getID());
        if (tache != null) {
            try {
                tache.annuler();
            } catch (Exception ex) {
                Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.mapJournalTache.remove(j.getID());
    }

    /**
     * *
     * annule la tache pour le flux envoyé et supprime le flux de la {@link #mapFluxTache}. En bref, le flux est retirée
     * du service
     *
     * @param f
     * @throws IncompleteBeanExeption
     */
    public synchronized void retirerFluxDuService(Flux f) throws IncompleteBeanExeption {
        if (f == null) {
            throw new NullPointerException("le flux est null");
        }
        if (f.getID() == null) {
            throw new IncompleteBeanExeption("Il n'est pas possible d'enregistrer un flux à L'ID NULL");
        }

        // On annule la tache

        List<AbstrTacheSchedule> listtacheDuFLux = this.mapFluxTache.get(f.getID());
        if (listtacheDuFLux != null) {
            for (int i = 0; i < listtacheDuFLux.size(); i++) {
                AbstrTacheSchedule abstrTacheSchedule = listtacheDuFLux.get(i);
                try {
                    abstrTacheSchedule.annuler();
                } catch (Exception ex) {
                    Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }


        this.mapFluxTache.remove(f.getID());

    }

    /**
     * *
     * Update le service de récupération. Plusieurs observable sont gérés : <ul>
     * <li>Les flux : lors de l'ajout de la modification ou suppression, le service doit être informé afin de recharger
     * son pool de thread en conséquence</li>
     * <li>Tache de récup : A la fin de l'éxecution d'une tâche de récupération, le service est notifié. Il décide si il
     * faut schedule la tache, cad le remettre dans son pool. Si la tache est en échec (présence d'une exeption), il
     * faut appel au service de gestion des incidents</li>
     * <li>@deprecated Conf Maintenant la conf ne se met plus a jour il faut redémarrer l'application pour recharger les
     * paramettres</li>
     * </ul>
     * <p>Le deuxieme argument permet de préciser les actions au service. C'est notamment utile lors de la modifiction
     * ou ajour d'un flux. Les actions suivantes doivent être gérée : <ul>
     * <li>add</li>
     * <li>mod</li>
     * <li>rem</li>
     * </li>reload all : permet de recharger completement le service</li>
     * </ul>
     *
     * </p>
     *
     *
     * @param o : L'observable se notifiant auprès du service (Flux, Conf, Tache)
     * @param arg Une précision sur l'action : une chaine de caractère exemple : add, mod, del.
     */
    @Override
    public void update(Observable o, Object arg) {


        /**
         * *========================================================================================
         * ........................Ajout ou modification d'un FLUX
         *///========================================================================================
        //Lorsque l'utilisateur modifi les flux, le service de collecte doit en être informé. 
//        if (o instanceof Flux) {
//            Flux flux = (Flux) o;
//            // Si c'est flux a ajouter
//            if (arg instanceof String && arg.equals("add")) {
//                try {
//                    enregistrerFluxAupresDuService(flux);
//
//                } catch (IncompleteBeanExeption ex) {
//                    Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//            if (arg instanceof String && arg.equals("mod")) {
//                try {
//                    this.enregistrerFluxAupresDuService(flux);
//                } catch (IncompleteBeanExeption ex) {
//                    Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }
        /**
         * *========================================================================================
         * .........................GESTION DU RETOUR DES TACHES SCHEDULE
         *///========================================================================================
        if (o instanceof AbstrTacheSchedule) {

            //-----------------------------------TACHE DE RECUPÉRATION DES FLUX-----------------------------
            if (o.getClass().equals(TacheRecupCallable.class)) {
                logger.debug("reception du retour d'un flux");
                TacheRecupCallable tache = (TacheRecupCallable) o;

                if (tache.getExeption() == null) {// Si la tâche s'est déroulée correctement
                    try {
                        tache.fermetureIncident(); // Si la tâche s'est terminé correctement, il faut fermer les incident si ils existent
                    } catch (Exception ex) {
                        Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (tache.getTacheSchedule() && !tache.annulerTache) { // On reschedule la tache normalement
//                        executorService.schedule(tache, tache.getFlux().getMediatorFlux().getPeriodiciteCollecte(), TimeUnit.SECONDS);
                        schedule(tache);
                    }

                } else { // Si la tâche est en erreur
                    try {
                        tache.gererIncident();

                    } catch (InstantiationException ex) {
                        logger.error("erreur lors de la gestion de l'incident de la tâche de collecte", ex);
                    } catch (IllegalAccessException ex) {
                        logger.error("erreur lors de la gestion de l'incident de la tâche de collecte", ex);
                    } catch (UnIncidableException ex) {
                        logger.error("erreur lors de la gestion de l'incident de la tâche de collecte", ex);
                    } catch (Exception ex) {
                        logger.error("erreur lors de la gestion de l'incident de la tâche de collecte", ex);
                    }
                    // TODO : On peut la reschedule dans 5 min si le nbr d'erreur est <3
                    if (tache.getTacheSchedule() && !tache.annulerTache) { // On reschedule la tache normalement 
//                            executorService.schedule(tache, tache.getFlux().getMediatorFlux().getPeriodiciteCollecte(), TimeUnit.SECONDS);
                        this.schedule(tache);

                    }
                }

            } //---------------------------Tache générale de vérification de la capture---------------------------------
//            else if (o.getClass().equals(TacheVerifComportementFluxGeneral.class)) {
//                TacheVerifComportementFluxGeneral cast = (TacheVerifComportementFluxGeneral) o;
//                
//                if (cast.schedule) {
////                    DateTime dtCurrent = new DateTime();
////                    DateTime next = dtCurrent.plusDays(1).withHourOfDay(2);// withDayOfWeek(DateTimeConstants.SUNDAY);
////                    Duration dur = new Duration(dtCurrent, next);
////                    executorService.schedule(cast, dur.getStandardSeconds(), TimeUnit.SECONDS);
//                    schedule(cast);
//                }
//            } //------------------------Tache de vérification de la capture pour un flux
            else if (o.getClass().equals(TacheVerifComportementFLux.class)) {
//                TacheVerifComportementFLux cast = (TacheVerifComportementFLux) o;
//                if (cast.getExeption() == null) {
//                    if (cast.getAnomalie()) { // Si la tache a déterminée une annomalie de capture
//                        AnomalieCollecte anomalie = new AnomalieCollecte();
//                        anomalie.setDateDebut(new Date());
//                        anomalie.setFluxLie(cast.getFlux());
//                        anomalie.feedMessageFromTask(cast);
//                        
//                        try {
//                            DAOIncident dao = (DAOIncident) DAOFactory.getInstance().getDaoFromType(AnomalieCollecte.class);
//                            dao.beginTransaction();
//                            dao.creer(anomalie);
//                            dao.commit();
//                        } catch (Exception ex) {
//                            logger.error("Erreur de la tâche : " + cast + ". Flux : " + cast.getFlux() + ". Lors de la création de l'anomanie : " + anomalie, ex);
//                            Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                }
            } else if (o.getClass().equals(TacheCalculQualiteFlux.class)) {
                TacheCalculQualiteFlux cast = (TacheCalculQualiteFlux) o;
                if (cast.getExeption() == null) {
                    cast.getFlux().setIndiceQualiteCaptation(cast.getIndiceCaptation());
                    cast.getFlux().setIndiceDecileNbrItemJour(cast.getDecile());
                    cast.getFlux().setIndiceMedianeNbrItemJour(cast.getMediane());
                    cast.getFlux().setIndiceQuartileNbrItemJour(cast.getQuartile());
                    cast.getFlux().setIndiceMinimumNbrItemJour(cast.getMinimum());
                    cast.getFlux().setIndiceMaximumNbrItemJour(cast.getMaximum());
                    try {
                        // Il faut enregistrer le résultat. 
                        DAOFactory.getInstance().getDAOFlux().beginTransaction();
                        DAOFactory.getInstance().getDAOFlux().modifier(cast.getFlux());
                        DAOFactory.getInstance().getDAOFlux().commit();
                    } catch (Exception ex) {
                        logger.error("Erreur de la tâche " + cast + " lors de la modification du flux " + cast.getFlux(), ex);

                    }
                }
            } else if (o.getClass().equals(TacheDecouverteAjoutFlux.class)) {
                TacheDecouverteAjoutFlux cast = (TacheDecouverteAjoutFlux) o;
                if (cast.getSchedule()) { // Si c'est une tâche schedulé on la replanifie.
                    schedule(cast);
                }
                if (cast.getExeption() != null) {
                    try {
                        cast.gererIncident();
                    } catch (Exception ex) {
                        logger.error("Impossible de gérer l'incident", ex);
                    }
                }

            }
        }
    }

    /**
     * *
     * Stope le service de collecte en fermant proprement les deux pool de tâches de collecte
     */
//    public void stopCollecte() {
//        // Fermeture du scheduler
//        this.executorService.shutdownNow();
//        this.poolPrioritaire.shutdownNow();
//    }
    /**
     * *
     * Cette méthode n'est maintenant plus utilisée au profit de majManuellAll()
     *
     * @param flux
     * @throws Exception
     * @deprecated
     */
    @Deprecated
    public void majManuelle(Flux flux) throws Exception {
        System.out.println("");
        TacheRecupCallable task = new TacheRecupCallable(flux, this, false);

        Future<TacheRecupCallable> t = this.poolPrioritaire.submit(task);

        t.get(30, TimeUnit.SECONDS);
        // A la fin de la tache, il faut rafraichir le context objet et la base de donnée.
//            DAOFactory.getInstance().getEntityManager().refresh(flux);
    }

    /**
     * *
     * Cette méthode lance la mise à jour manuelle de chacun des flux envoyés en parametres
     *
     * @param listFlux Liste de flux pour lequels il faut lancer une mise à jour manuelle
     * @throws Exception
     */
    public List<TacheRecupCallable> majManuellAll(List<Flux> listFlux) throws Exception {
        int i;

        List<TacheRecupCallable> listTache = new ArrayList<TacheRecupCallable>();
        for (i = 0; i < listFlux.size(); i++) {
            TacheRecupCallable task = new TacheRecupCallable(listFlux.get(i), this, false);
            listTache.add(task);
            
//            listFlux.get(i).setTacheRechupManuelle(task);
        }
        DateTime dtDebut = new DateTime();
        

        List<Future<TacheRecupCallable>> listFutur = this.poolPrioritaire.invokeAll(listTache);
      
        
        return listTache;
    }

    /**
     * *
     * Permet d'ajouter un callable au pool schedulé. La méthode scheduleAtFixedRate ne permet pas d'ajouter des
     * Callable, seulement des runnable. Pour cette raison, les renable doivent se réajouter en fin de tache pour avoir
     * un effet scheduleAtFixedRate
     *
     * @param t Le RUNNABLE qui doit être ajouté au pool
     */
//    public void addScheduledCallable(TacheRecupCallable t) {
////        this.poolSchedule.schedule(t, t.getFlux().getPeriodiciteCollecte(), TimeUnit.SECONDS);
//        this.executorService.schedule(t, t.getFlux().getMediatorFlux().getPeriodiciteCollecte(), TimeUnit.SECONDS);
//    }
    /**
     * *
     * Retoune le pool prioritaire du service. Il s'agit du pool pour lancer des collectes manuelle. Celles ci sont
     * lancée avec une priorité suppréieure au pool schedulé
     *
     * @return
     */
    public ExecutorService getPoolPrioritaire() {
        return poolPrioritaire;
    }

    /**
     * *
     * Définir le pool prioritaire
     *
     * @param poolPrioritaire
     */
    public void setPoolPrioritaire(ExecutorService poolPrioritaire) {
        this.poolPrioritaire = poolPrioritaire;
    }

//    @Override
    public void lancerCollecte() {

        // On charge le cache
        cacheHashFlux.ChargerLesHashdesFluxdepuisBDD(); // Au démarrage du service, il faut charger les hash pour tout les flux dans le cache


        //---------------TACHES DE COLLECTE--------------

        List<Flux> listf = DAOFactory.getInstance().getDAOFlux().findAllFlux(Boolean.TRUE);
        for (int i = 0; i < listf.size(); i++) {
            Flux flux = listf.get(i);
            try {
                enregistrerFluxAupresDuService(flux);
            } catch (IncompleteBeanExeption ex) {
                logger.error("erreur lors de l'enregistrement du flux ");
                Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        // On doit récupérer les journaux permettant un ajout périodique
        DaoJournal daoJournal = DAOFactory.getInstance().getDaoJournal();
        List<Journal> journaux = daoJournal.findall();
        for (int i = 0; i < journaux.size(); i++) {
            Journal journal = journaux.get(i);

            if (journal.getAutoUpdateFlux() != null && journal.getAutoUpdateFlux()) {
                try {
                    enregistrerJournalAupresduService(journal);
                } catch (IncompleteBeanExeption ex) {
                    Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        //----------------TACHE TacheVerifComportementFluxGeneral
//        TacheVerifComportementFluxGeneral comportementFluxGeneral = new TacheVerifComportementFluxGeneral(this);
//        DateTime dtCurrent = new DateTime();
//        DateTime next = dtCurrent.plusDays(1).withHourOfDay(2);// withDayOfWeek(DateTimeConstants.SUNDAY);
//        Duration dur = new Duration(dtCurrent, next);
//        this.executorService.schedule(comportementFluxGeneral, dur.getStandardSeconds(), TimeUnit.SECONDS);

    }

    @Override
    protected void gererIncident(AbstrTacheSchedule tache) {
//        // Si la tâche est incidable et si il y a une exeption
//        if (tache.exeption != null && Incidable.class.isAssignableFrom(tache.getClass())) {
//
//            CollecteIncident si = null;
//            Flux fluxConcerne = null; // On cherche si il y a déjà un incident ouvert pour le flux.
//            Throwable exception = tache.getExeption();
//
//            if (exception instanceof ExecutionException) {
//                ExecutionException cast = (ExecutionException) exception;
//                if (cast.getCause() != null && Exception.class.isAssignableFrom(cast.getCause().getClass())) {
//                    exception = (Exception) cast.getCause();
//                }
//            }
//
//            //================================================================================================
//            //                      INSTANCIATION OU RECUPERATION D'INCIDENT
//            //================================================================================================
//
//            if (tache.getClass().equals(TacheRecupCallable.class)) {
//                TacheRecupCallable castTache = (TacheRecupCallable) tache;
//                fluxConcerne = castTache.getFlux();
//                castTache.em.refresh(fluxConcerne); // Il faut rafraichir le flux
////                DAOFactory.getInstance().getEntityManager().refresh(fluxConcerne);
//                si = fluxConcerne.getIncidentOuverType(CollecteIncident.class);
//                logger.debug("recup des incid . Si : " + si);
//            }
//
//            if (si == null) {
//                IncidentFactory factory = new IncidentFactory();
//                try {
//                    si = (CollecteIncident) factory.createIncidentFromTask(tache, "blabla");
//                } catch (InstantiationException ex) {
//                    logger.error("Erreur d'instanciation de l'incident. Pour la tache" + tache, ex);
//                } catch (IllegalAccessException ex) {
//                    logger.error("Erreur d'instanciation de l'incident. Pour la tache" + tache, ex);
//                } catch (UnIncidableException ex) {
//                    logger.debug("La tâche n'est pas incidable");
//                }
//            }
//
//            //=================================================================================================
//            // ..................... GESTION DES INCIDENTS
//            //=================================================================================================
//            if (si != null) {
//                if (tache.getClass().equals(TacheRecupCallable.class)) {
//                    TacheRecupCallable cast = (TacheRecupCallable) tache;
//                    logger.debug("Erreur lors de la récupération du flux  : " + cast.getFlux() + ". Erreur : " + cast.getExeption(), cast.getExeption());
//                    si.setFluxLie(fluxConcerne);
//
//                    if (si.getDateDebut() == null) {
//                        si.setDateDebut(new Date());
//                    }
//
//                    Integer nbr = si.getNombreTentativeEnEchec();
//                    nbr++;
//                    si.setNombreTentativeEnEchec(nbr);
//                    si.setLogErreur(exception.toString());
//
//                    if (exception instanceof HTTPException) {
//                        HTTPException ex = (HTTPException) exception;
//                        si.setMessageEreur("HTTPException : Erreur sur le flux " + fluxConcerne + ". Le serveur est joingnable mais retour d'un code erreur : " + ex.getStatusCode());
//                    } else if (exception instanceof UnknownHostException) {
//                        si.setMessageEreur("UnknownHostException : Il est impossible de joindre l'host du flux");
//                    } else if (exception.getClass().equals(ParsingFeedException.class)) {
//                        si.setMessageEreur("ParsingFeedException : Impossible de parser le flux XML.");
//                    } else if (exception instanceof FeedException) {
//                        si.setMessageEreur("FeedException : Impossible de parser le flux XML.");
//                    } else if (exception instanceof CollecteUnactiveFlux) {
//                        logger.info("Tentative de collecte d'un flux innactif, ce n'est surement pas grave");
//                    } else if (exception instanceof Exception) {
//                        si.setMessageEreur("ERREUR inconnue : " + exception.toString());
//                    }
//                    cast.setIncident(si);
//                }
//
//                //=================================================================================================
//                //...............................Enregistrment de l'incident
//                //=================================================================================================
//
////                DAOIncident dao = (DAOIncident) DAOFactory.getInstance().getDAOFromTask(tache);
//                try {
//                    logger.debug("avant enregistrement");
//                    AbstrServiceCRUD serviceCRUD = ServiceCRUDFactory.getInstance().getServiceFor(si.getClass());
//                    if (si.getID() == null) {
//                        logger.debug("Creation d'un incident");
//
//                        // On récupère le service 
//                        serviceCRUD.ajouter(si);
//                    } else {
//                        logger.debug("MAJ d'un incident");
//                        serviceCRUD.modifier(si);
//                        System.out.println("NBR tentative : " + si.getNombreTentativeEnEchec());
//                    }
//                } catch (Exception ex) {
//                    logger.error("Erreur lors de la création de l'incident : " + si, ex);
//                }
//            }
//        }
//
//        //=================================================================================================
//        //.........................Terminaison correct des TACHE et FERMETURE DE L'INCIDENT
//        //=================================================================================================
//        //Si la tâche s'est déroulée correctement. Il est peut être nécessaire de fermer des incident
//        if (tache.exeption == null) {
//            if (tache.getClass().equals(TacheRecupCallable.class)) {
//
//                TacheRecupCallable cast = (TacheRecupCallable) tache;
//                logger.debug("La récuperation du flux " + cast.getFlux() + ". S'est déroulée correctement. Fermeture des possibles incidents");
//                Flux f = cast.getFlux();
//                cast.em.refresh(f);
//
//                List<CollecteIncident> listIncid = f.getIncidentEnCours();
//                for (int i = 0; i < listIncid.size(); i++) {
//                    CollecteIncident collecteIncident = listIncid.get(i);
//                    collecteIncident.setDateFin(new Date());
//                    AbstrServiceCRUD serviceCRUD = ServiceCRUDFactory.getInstance().getServiceFor(collecteIncident.getClass());
//                    try {
//                        System.out.println("MODIF ");
//                        serviceCRUD.modifier(collecteIncident);
//                    } catch (Exception ex) {
//                        logger.error("Erreur la modification de l'incident : " + collecteIncident, ex);
//                    }
//                }
//            }
//        }
    }

    /**
     * *
     * Cette méthode permet d'ajouter un item à un flux. Si l'item est déja présente dans la base de donnée, le service
     * crée un liaison vers cette item. Sinon il la crée. La méthode est synchronisé afin que plusieurs thread
     * n'ajoutent pas en même temps des items.
     *
     * @param flux Le flux pour lequel il faut ajouter une item
     * @param item L'item devant être ajouté
     */
    public synchronized void ajouterItemAuFlux(Flux flux, Item item, EntityManager em, Boolean commiter) {

        DaoItem dao = DAOFactory.getInstance().getDaoItem();

        if (em == null) {
            em = DAOFactory.getInstance().getEntityManager();
            em.getTransaction().begin();

        }
        dao.setEm(em);

        System.out.println("--AJOUT");


//
//        if (em != null) {
//            dao.setEm(em);
//            if (!em.isJoinedToTransaction()) {
//                em.getTransaction().begin();
//            }
//        } else {
//            dao.beginTransaction();
//        }

//        em.lock(item, LockModeType.PESSIMISTIC_WRITE);



        // On commence par ajouter l'item au flux si ce n'est pas déjà le cas
        List<Flux> lf = item.getListFlux();
        Boolean ajouter = true;
        for (int i = 0; i < lf.size(); i++) {
            Flux flux1 = lf.get(i);
            if (flux1.getID().equals(flux.getID())) {
                ajouter = false;
            }
        }
        if (ajouter) {
            item.getListFlux().add(flux);
        }

        Boolean itemEstNouvelle = true;
        Boolean err = false;
        if (item.getID() != null) { // Une item possédant un ID n'est pas nouvelle, il faut alors changer le booleean
            itemEstNouvelle = false;
        }
        logger.debug("item nouvelle : " + itemEstNouvelle);
        //
        if (itemEstNouvelle) {  // Si l'item est nouvelle, on va effectuer une création dans la base de données
            try {
                dao.creer(item);
            } catch (Exception ex) {
                err = true;
                logger.debug("erreur lors de l'ajout", ex);
            }
        }

        // Si l'item n'est pas nouvelle ou si il y a eu une erreur lors de l'enregistrement précédent, Il faut retrouver l'item dans la base de donnée et l'aéjouter si besoin est au flux
        if (!itemEstNouvelle || err) {
            Item itBDD = dao.findByHash(item.getHashContenu());
            // Si on a bien trouvé une item
            if (itBDD != null) {
                // On ajoute l'item au flux 
                itBDD.getListFlux().add(flux);
                try {
                    // On tente de modifier l'item 
                    dao.modifier(itBDD);
                    err = false;
                } catch (Exception ex) {
                    Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        // Si le traitement s'est bien déroulé
        if (!err) {
            try {
                if (commiter) { // Si aucun em n'a été précisé alors c'est de la responsabilité du service de demander le comit. Sinon c'est l'appelan qui doit se débrouiller avec son em
                    dao.commit(); // On commit
                    cacheHashFlux.addHash(flux, item.getHashContenu());
                }

            } catch (Exception e) {
                logger.error("erreur lors du commit", e);
            }
        }
        // Il faudra trouver qqchose en cas a nouveau d'erreur exemple la base de de donnée ne répond pas.

    }

    /**
     * *
     * Méthode permettant de supprimer le flux. L'ensemble des items du flux sont parcourues. Si les items sont seules,
     * elle sont supprimées. Si elles appartiennent à un autre flux, on retire le flux de l'item, puis on modifie
     * l'item.
     *
     * @param flux
     */
    @Deprecated //----> C'est maintenant dans le service CRUD
    public void removeFluxWithItem(Flux flux) throws Exception {

        DaoItem daoItem = DAOFactory.getInstance().getDaoItem();
        daoItem.beginTransaction();
        Boolean err = false;


        List<Item> items = daoItem.itemLieAuFlux(flux);

        int i;
        for (i = 0; i < items.size(); i++) {
            Item item = items.get(i);

            //Supppression des items qui vont devenir orphelines
            if (item.getListFlux().size() < 2) {
                // On supprimer la relation 
                item.getListFlux().clear();
                try {
                    daoItem.modifier(item);
                    daoItem.remove(item);
                } catch (Exception e) {
                    err = true;
                    logger.debug("Erreur lors de la suppression", e);
                }

            } else { // Sinon on détach le flux
                item.getListFlux().remove(flux);

                try {
                    daoItem.modifier(item);
                } catch (Exception ex) {
                    err = true;
                    logger.debug("Erreur lors de la modification", ex);
                }
            }
        }
        // On va supprimer le flux si la procédure de suppression des items s'est déroulée correctement
        flux.setItem(new ArrayList<Item>());

        DaoFlux daoFlux = DAOFactory.getInstance().getDAOFlux();
        daoFlux.beginTransaction();

        try {
            daoFlux.remove(flux);
        } catch (IllegalArgumentException ex) {
            err = true;
            Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransactionRequiredException ex) {
            err = true;
            Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            err = true;
            Logger.getLogger(ServiceCollecteur.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Tous le monde est OK, Alors on commit
        if (!err) {
            System.out.println("COMMIT");
            try {
                daoItem.commit();
                System.out.println("1");
            } catch (Exception e) {
                logger.debug("Erreur lors du comit de l'item", e);
            }

            try {
                daoFlux.commit();
                cacheHashFlux.removeFlux(flux);
                System.out.println("2");
            } catch (Exception e) {
                logger.debug("erreur", e);
            }

        } else {
            throw new Exception("Erreur lors de la suppression");
        }
    }

    /**
     * *
     * Permet de lancer la découverte des flux RSS d'un journal
     *
     * @param journal
     * @param persisterAjout précise si les flux découverts doivent être persisté
     * @param activerFlux précise si les flux découvert doivent être activé (il faut déjà qu'il soient persisté
     * @throws IncompleteBeanExeption Si le journal est null n'a pas d'ID ou si il ne possède pas de champs
     * {@link Journal#urlHtmlRecapFlux}
     */
    public Future<TacheDecouverteAjoutFlux> decouverteFluxJournal(Journal journal, Boolean persisterAjout, Boolean activerFlux) throws IncompleteBeanExeption, DonneeInterneCoherente {
        if (journal == null) {
            throw new NullPointerException("Le journal est null");
        }
        if (journal.getID() == null) {
            throw new IncompleteBeanExeption("Le beans n'a pas d'ID");
        }
        if (journal.getUrlHtmlRecapFlux() == null || journal.getUrlHtmlRecapFlux().isEmpty()) {
            throw new IncompleteBeanExeption("Le journal ne possède pas de champs URLHTMLRECAP. Impossible de découvrir les flux");
        }

        if (!persisterAjout && activerFlux) {
            throw new DonneeInterneCoherente("Il est impossible d'activer des flux non persisté");
        }



        TacheDecouverteAjoutFlux tache = new TacheDecouverteAjoutFlux();
        tache.addObserver(this);
        tache.setJournal(journal);
        tache.setNombredeSousTache(30);
        tache.setActiverLesFLux(activerFlux);
        tache.setPersist(persisterAjout);
        Future<TacheDecouverteAjoutFlux> fut = executorService.submit(tache);
        return fut;

//        return tache;


    }

    @Override
    public void stopService() throws SecurityException, RuntimeException {
        if (this.poolPrioritaire != null) {
            this.poolPrioritaire.shutdownNow();
        }
        super.stopService();
    }

    public CacheHashFlux getCacheHashFlux() {
        return cacheHashFlux;
    }

    public void setCacheHashFlux(CacheHashFlux cacheHashFlux) {
        this.cacheHashFlux = cacheHashFlux;
    }
}
