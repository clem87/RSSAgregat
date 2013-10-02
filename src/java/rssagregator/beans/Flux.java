package rssagregator.beans;

import rssagregator.services.TacheRecupCallable;
import com.sun.syndication.feed.opml.Attribute;
import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import rssagregator.beans.traitement.MediatorCollecteAction;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import rssagregator.beans.incident.CollecteIncident;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.config.CacheIsolationType;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import rssagregator.beans.exception.DonneeInterneCoherente;
import rssagregator.dao.DAOFactory;
import rssagregator.dao.DaoFlux;
import rssagregator.services.ServiceCollecteur;
import rssagregator.services.TacheCalculQualiteFlux;
import rssagregator.services.TacheVerifComportementFLux;

/**
 * Une des entités les plus importantes... Il s'agit d'un flux de syndication (RSS ATOM...). Un flux appartient est lié
 * aux entitées suivante : <ul>
 * <li>{@link Journal} : Un flux peut appartenir à 0 à 1 journal</li>
 * <li>{@link FluxType} : Un flux peut appartenir à 0 à 1 type de flux (international, à la une ...)</li>
 * <li>{@link MediatorCollecteAction} : Une flux possède un comportement de collecte permettant de collecter les
 * {@link Item}</li>
 * <li>{@link Item} : Un flux possède 0 à N item. </li>
 * <li>{@link CollecteIncident} : un flux peut posséder 0 à N incidents de Collecte.</li>
 * </ul>
 */
@Entity
@Table(name = "flux")
@Cacheable(value = true)
@Cache(type = CacheType.FULL, coordinationType = CacheCoordinationType.SEND_NEW_OBJECTS_WITH_CHANGES, isolation = CacheIsolationType.SHARED)
public class Flux extends AbstrObservableBeans implements Observer, Serializable, BeanSynchronise {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long ID;
    /**
     * URL du flux rss sous la forme http://url/rep.
     */
    @Column(name = "url", length = 2000, nullable = false, unique = true)
    private String url;
    /**
     * Permet de déterminer si le flux doit être collecté ou non
     */
    @Column(name = "active")
    /**
     * Permet de déterminer si le flux doit être collecté ou non
     */
    protected Boolean active;
    /**
     * L'url de la rubrique du flux, il s'agit de la page HTML d'entrée de la rubrique. Cette adresse peut être utilisé
     * pour faire de l'auto discovery.
     */
    @Column(name = "htmlUrl", length = 2000, nullable = true)
    private String htmlUrl;
    @Transient
    /**
     * Les dernières empruntes md5 des items du flux. On les garde en mémoire pour faire du dédoublonage sans effectuer
     * de requetes dans la base de données. On ne persiste pas dans la base de donnée (TRANSISIENT NORMAL)
     */
    private List<String> lastEmpruntes;
    /**
     * L'objet Callable qui permet d'être lancé pour effectuer la récupération du flux. Cet objet doit être ajouté dans
     * le pool de thread du service de récupération. Il n'est pas persisté
     */
    @Transient
    private TacheRecupCallable tacheRechup;
    /**
     * *
     * Cette tâche de récupération est utilisée lorsque l'utilisateur demande manuellement la mise à jour du flux
     */
    @Transient
    private TacheRecupCallable tacheRechupManuelle;
    /**
     * Lors du lancement d'une collecte par la tache de récupération, cette liste est mise à jour lorsque à la fin de la
     * collecte, la tache notifie au flux ses résultats. On trouve dans ces résultats des items déja collecte, ils sont
     * marqué du bool item.isNew.
     */
    @Deprecated
    @Transient
    private List<Item> listDernierItemCollecte;
    /**
     *
     * Liste des Item du flux. La relation est possédée par l'item !! Il faut passer par la DAO pour obtenir la liste
     * des items. Item
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "listFlux")
    private List<Item> item;
    /**
     * Un objet flux peut posséder différents incidents. Un incident ne possède qu'un flux.
     *
     * @element-type CollecteIncident
     */
//    @OneToMany(mappedBy = "flux", cascade = CascadeType.ALL)
//    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true,fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "fluxLie", cascade = {CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CollecteIncident> incidentsLie;
    /**
     *
     * @element-type InfoCollecte
     */
//Cascade remove : On va utiliser la dao flux pour sauvegarder les infos collecte. 
//    @OneToMany(mappedBy = "flux", cascade = {CascadeType.ALL})
//    @Deprecated // Idée abandonnée. Il est déjà possible de commenter les incident qui sont daté; Le commentaire sur le flux sera finalement un simple champ texte
//    @Transient
//    private List<InfoCollecte> infoCollecteFlux;
    /**
     * *
     * Un champs informatif dans lequel les administrateurs peuvent saisir des commentaires sur le flux.
     */
    @Column(name = "infoCollecte", columnDefinition = "text")
    private String infoCollecte;
    /**
     * Le type du flux (international, a la une etc...). Les types de flux sont des beans. ils sont persisté dans la
     * base de données
     */
//    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.DETACH})
    @CascadeOnDelete
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
    private FluxType typeFlux;
    /**
     * Un flux peut appratenir à un journal. Un journal peut contenir plusieurs flux
     */
// On veut que le flux ne puisse pas créer de journaux mais simplment se lier. Ce n'est pas à la dao du flux de de créer des journaux.
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Journal journalLie;
    /**
     * Le mediator flux permet d'assigner un flux un comportement de collecte. Un médiator est une configuration de
     * parseur Raffineur etc. IL s4AGIT DU COMPTEMENT DE COLLECTE ACTUELLE. Si auparavant d'autres comportement ont été
     * utilisé, il sont visibles dans les période de captations.
     */
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private MediatorCollecteAction mediatorFlux;
    /**
     * *
     * C'est une copie du modèle.
     */
    @Transient
    private MediatorCollecteAction mediatorFluxAction;
    /**
     * On ne persiste pas ce champs
     */
    @Transient
    private Boolean erreurDerniereLevee;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date created;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date modified;
    /**
     * *
     * Un flux peut être le sous flux d'un autre, exemple Europe est un sous flux de international. Si null, il s'agit
     * d'un flux racine
     */
    @OneToOne
    Flux parentFlux;
    /**
     * *
     * Un nom pour le flux. Ce champ est utilisé par la méthode toString. Si ce champ est vide tostring va chercher le
     * journal et le type de flux. Sinon il va montrer l'url
     */
    @Column(name = "nom")
    private String nom;
    /**
     * Retourne une la liste des rss autodécouvert. Commence par l'adresse urlRubrique. Si pas de réponse, on remonte
     * vers la racine du site. On enlève une sous répertoire par tentative.
     */
    // TODO : supprimer ceci à la fin
    /**
     * *
     * Utilise pour le debug. A chaque levée on ajoute la date, permet de bien vérifier que les flux sont levee en
     * permanence
     */
    @Transient
    List<DebugRecapLeveeFlux> debug;
    /**
     * *
     * Un indice de 0.0 à 100.0 permettant de mesurer la disponibilité du flux durant la captation. Cet indice est
     * calculé par la tâche {@link TacheCalculQualiteFlux} qui observe la durée de la période de calpation et le cumul
     * de la durée des incidents.
     */
    @Column(name = "indiceQualiteCaptation")
    protected Float indiceQualiteCaptation;
    /**
     * *
     * Médiane du nombre d'item jour capturé pour le flux. Cet indice est calculé par la tâche
     * {@link TacheCalculQualiteFlux}
     */
    protected Integer indiceMedianeNbrItemJour;
    /**
     * *
     * Décile du nombre d'item jour capturé pour le flux. Cet indice est calculé par la tâche
     * {@link TacheCalculQualiteFlux}
     */
    protected Integer indiceDecileNbrItemJour;
    /**
     * *
     * Quartile du nombre d'item jour capturé pour le flux. Cet indice est calculé par la tâche
     * {@link TacheCalculQualiteFlux}
     */
    protected Integer indiceQuartileNbrItemJour;
    /**
     * *
     * Maximum du nombre d'item jour capturé pour le flux. Cet indice est calculé par la tâche
     * {@link TacheCalculQualiteFlux}
     */
    protected Integer indiceMaximumNbrItemJour;
    /**
     * *
     * Minimum du nombre d'item jour capturé pour le flux. Cet indice est calculé par la tâche
     * {@link TacheCalculQualiteFlux}
     */
    protected Integer indiceMinimumNbrItemJour;
    /**
     * *
     * Liste des périodes durant lesquel un flux à été capturé (boolean active à true). Les période de captation sont
     * des entitées persisté dans la base de données possédant deux dates. Durant sa période de captation, un flux peut
     * être en échec (posséder des incident {@link CollecteIncident}
     */
    @OneToMany(mappedBy = "flux", cascade = CascadeType.ALL)
    protected List<FluxPeriodeCaptation> periodeCaptations;
    /**
     * *
     * Les incident en cours sont gardée en mémoire mais pas persisté. Il faut les charger au démarrage.
     */
//    @Transient
//    List<FluxIncident> incidentEnCours;
    /**
     * *
     *
     * Variable qui permet à l'utilisateur de qualifié le flux de stable. On considère qu'il est stable si le flux ne
     * subit pas trop d'anomalie et qu'il renvoie un nombre d'item assez régulier. Les flux qualifié de stable son sujet
     * a être vérifier par les tache de verification de comportement ({@link TacheVerifComportementFLux). On peut qualifier un flux de non stable pour éviter les
     * notification abusive. Un flux non stable continu tout de même à être
     * revevé
     */
    protected Boolean estStable;

    public List<DebugRecapLeveeFlux> getDebug() {
        return debug;
    }

    public void setDebug(List<DebugRecapLeveeFlux> debug) {
        this.debug = debug;
    }

    @Deprecated
    public void autodiscovery() {
    }

    /**
     * Test la config en récupérant les items à l'url. Le contenu n'est pas persister. Permet à l'admin de s'assurer que
     * la config est bonne, que le parseur est adhéquat
     */
    public void Obsolete_test() {
    }

    /**
     * Le constructeur
     */
    public void Flux() {
        this.item = new ArrayList<Item>();
        this.listDernierItemCollecte = new ArrayList<Item>();
    }

    public MediatorCollecteAction getMediatorFluxAction() {
        return mediatorFluxAction;
    }

    public void setMediatorFluxAction(MediatorCollecteAction mediatorFluxAction) {
        this.mediatorFluxAction = mediatorFluxAction;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

//    public Integer getPeriodiciteCollecte() {
//        return periodiciteCollecte;
//    }
//
//    public void setPeriodiciteCollecte(Integer periodiciteCollecte) {
//        this.periodiciteCollecte = periodiciteCollecte;
//    }
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getUrlRubrique() {
        return htmlUrl;
    }

    public void setUrlRubrique(String urlRubrique) {
        this.htmlUrl = urlRubrique;
    }

    public List<String> getLastEmpruntes() {
        return lastEmpruntes;
    }

    public void setLastEmpruntes(List<String> lastEmpruntes) {
        this.lastEmpruntes = lastEmpruntes;
    }

    /**
     * *
     * Pointeur vers la tache schedule permettant de récupérer le flux
     *
     * @return
     */
    public TacheRecupCallable getTacheRechup() {
        return tacheRechup;
    }

    /**
     * *
     * Pointeur vers la tache schedule permettant de récupérer le flux
     *
     * @param tacheRechup
     */
    public void setTacheRechup(TacheRecupCallable tacheRechup) {
        this.tacheRechup = tacheRechup;
    }

    public List<Item> getListDernierItemCollecte() {
        return listDernierItemCollecte;
    }

    public void setListDernierItemCollecte(List<Item> listDernierItemCollecte) {
        this.listDernierItemCollecte = listDernierItemCollecte;
    }

    public List<Item> getItem() {
        return item;
    }

    public void setItem(List<Item> items) {
        this.item = items;
    }

//    public String getInfoCollecte() {
//        return infoCollecte;
//    }
//
//    public void setInfoCollecte(String infoCollecte) {
//        this.infoCollecte = infoCollecte;
//    }
    public FluxType getTypeFlux() {
        return typeFlux;
    }

    public void setTypeFlux(FluxType typeFlux) {
        this.typeFlux = typeFlux;
    }

    public Journal getJournalLie() {
        return journalLie;
    }

    public void setJournalLie(Journal journalLie) {
        this.journalLie = journalLie;
    }

    public MediatorCollecteAction getMediatorFlux() {
        return mediatorFlux;
    }

    public void setMediatorFlux(MediatorCollecteAction MediatorFlux) {
        this.mediatorFlux = MediatorFlux;
    }

    public Flux getParentFlux() {
        return parentFlux;
    }

    public void setParentFlux(Flux parentFlux) {
        this.parentFlux = parentFlux;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public List<FluxPeriodeCaptation> getPeriodeCaptations() {
        return periodeCaptations;
    }

    public String getInfoCollecte() {
        return infoCollecte;
    }

    public void setInfoCollecte(String infoCollecte) {
        this.infoCollecte = infoCollecte;
    }

    public void setPeriodeCaptations(List<FluxPeriodeCaptation> periodeCaptations) {
        this.periodeCaptations = periodeCaptations;
    }

    public Flux() {

        this.debug = new ArrayList<DebugRecapLeveeFlux>();
        this.item = new ArrayList<Item>();
//        this.lastEmpruntes = new ArrayList<String>();
        this.lastEmpruntes = new LinkedList<String>();

        this.mediatorFlux = MediatorCollecteAction.getDefaultCollectAction();
        this.incidentsLie = new ArrayList<CollecteIncident>();
        this.periodeCaptations = new ArrayList<FluxPeriodeCaptation>();

//        this.incidentEnCours = new ArrayList<FluxIncident>();
        this.setChanged();

        // On enregistre le Flux auprès des services qu'il doit notifier lors de ses changement d'états
//        this.addObserver(ServiceCollecteur.getInstance());
//        this.addObserver(ServiceSynchro.getInstance());

    }

    public Flux(String url) {
        this();
        this.url = url;
//        this.debug = new ArrayList<DebugRecapLeveeFlux>();
//        this.item = new LinkedList<Item>();
////        this.lastEmpruntes = new ArrayList<String>();
//        this.lastEmpruntes = new LinkedList<String>();
//
//        this.mediatorFlux = MediatorCollecteAction.getDefaultCollectAction();
//        this.incidentsLie = new ArrayList<FluxIncident>();

//
////        this.periodiciteCollecte = 3600;
//        this.active = Boolean.TRUE;
//
//        this.incidentEnCours = new ArrayList<FluxIncident>();
//        this.setChanged();
//        
//                this.addObserver(ServiceCollecteur.getInstance());
//        this.addObserver(ServiceSynchro.getInstance());
    }

    @Override
    public void update(Observable o, Object arg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    /**
//     * *
//     * Créer une nouvelle tache de récupération pour le Flux
//     */
//    public void createTask() {
//        this.tacheRechup = new TacheRecupCallable(this);
//    }
    public Boolean getErreurDerniereLevee() {
        return erreurDerniereLevee;
    }

    public void setErreurDerniereLevee(Boolean erreurDerniereLevee) {
        this.erreurDerniereLevee = erreurDerniereLevee;
    }

    public Long getID() {
        return ID;
    }

    public void setID(Long ID) {
        this.ID = ID;
    }

    public List<CollecteIncident> getIncidentsLie() {
        return incidentsLie;
    }

    public void setIncidentsLie(List<CollecteIncident> incidentsLie) {
        this.incidentsLie = incidentsLie;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Boolean getEstStable() {
        return estStable;
    }

    public void setEstStable(Boolean estStable) {
        this.estStable = estStable;
    }

    public Float getIndiceQualiteCaptation() {
        return indiceQualiteCaptation;
    }

    public void setIndiceQualiteCaptation(Float indiceQualiteCaptation) {
        this.indiceQualiteCaptation = indiceQualiteCaptation;
    }

    public Integer getIndiceMedianeNbrItemJour() {
        return indiceMedianeNbrItemJour;
    }

    public void setIndiceMedianeNbrItemJour(Integer indiceMedianeNbrItemJour) {
        this.indiceMedianeNbrItemJour = indiceMedianeNbrItemJour;
    }

    public Integer getIndiceDecileNbrItemJour() {
        return indiceDecileNbrItemJour;
    }

    public void setIndiceDecileNbrItemJour(Integer indiceDecileNbrItemJour) {
        this.indiceDecileNbrItemJour = indiceDecileNbrItemJour;
    }

    public Integer getIndiceQuartileNbrItemJour() {
        return indiceQuartileNbrItemJour;
    }

    public void setIndiceQuartileNbrItemJour(Integer indiceQuartileNbrItemJour) {
        this.indiceQuartileNbrItemJour = indiceQuartileNbrItemJour;
    }

    public Integer getIndiceMaximumNbrItemJour() {
        return indiceMaximumNbrItemJour;
    }

    public void setIndiceMaximumNbrItemJour(Integer indiceMaximumNbrItemJour) {
        this.indiceMaximumNbrItemJour = indiceMaximumNbrItemJour;
    }

    public Integer getIndiceMinimumNbrItemJour() {
        return indiceMinimumNbrItemJour;
    }

    public void setIndiceMinimumNbrItemJour(Integer indiceMinimumNbrItemJour) {
        this.indiceMinimumNbrItemJour = indiceMinimumNbrItemJour;
    }

//    public void setIncidentEnCours(List<FluxIncident> incidentEnCours) {
//        this.incidentEnCours = incidentEnCours;
//    }
    /**
     * *
     * Parcours les incidents et retourne ceux qui ne sont pas clos, cad ceux qui n'ont pas de date de fin
     *
     * @return
     */
    public List<CollecteIncident> getIncidentEnCours() {
        List<CollecteIncident> incid = getIncidentsLie();
        List<CollecteIncident> retour = new ArrayList<CollecteIncident>();

        for (int i = 0; i < incid.size(); i++) {
            CollecteIncident fluxIncident = incid.get(i);
            if (fluxIncident.getDateFin() == null) {
                retour.add(fluxIncident);
            }
        }

        return retour;
//        List<FluxIncident> iRetour = new ArrayList<FluxIncident>();
//        
//        List<FluxIncident> fluxIncidents;
//        fluxIncidents = DAOFactory.getInstance().getDAOIncident().findByFlux(this.ID);
//
//        int i;
//        for (i = 0; i < this.incidentsLie.size(); i++) {
//            if (this.incidentsLie.get(i).getDateFin() == null) {
//                iRetour.add(this.incidentsLie.get(i));
//            }
//        }
//        return iRetour;
    }

    /**
     * *
     * Parcours les incident ouvert et retourne le premier incident ouvert du même type que la class envoyé en argument.
     *
     * @param c
     * @return L'incident ouvert du même type que Classc ou null si rien n'a été trouvé
     */
    public CollecteIncident getIncidentOuverType(Class c) {
//        List<AbstrFluxIncident> listRetour = new ArrayList<AbstrFluxIncident>();

        List<CollecteIncident> list = this.getIncidentEnCours();
        int i;
        for (i = 0; i < list.size(); i++) {
            if (list.get(i).getClass().equals(c)) {
                return list.get(i);
//                listRetour.add(list.get(i));
            }
        }
        return null;
    }

    /**
     * *
     * Parcours les incidents du flux et inscrit la date courante en date de fin d'un evenuel incident ouvert. Cette
     * méthode est éxecuté pour chaque flux losque la levée s'est déroulé avec succes
     */
    @Deprecated
    public void fermerLesIncidentOuvert() {
        int i;
        List<CollecteIncident> incidentOuvert = this.getIncidentEnCours();

        for (i = 0; i < incidentOuvert.size(); i++) {
            // On vérifi quand même que la date de fin est bien null
            if (incidentOuvert.get(i).getDateFin() == null) {
                incidentOuvert.get(i).setDateFin(new Date());
            }
        }
    }

    @Override
    /**
     * *
     * Retourne le nom du journal ainsi que le type du flux. Si ces variables ne sont pas définient, on retourne l'url.
     */
    public String toString() {

        // Si on a un nom on le retourne en priorité
        if (nom != null && !nom.trim().isEmpty()) {
            return nom.trim();
        } else if (this.getJournalLie() != null && this.getTypeFlux() != null) {
            return this.getJournalLie().getNom() + " - " + this.getTypeFlux().getDenomination();
        } else if (this.url != null && !this.url.isEmpty()) {
            return this.url;
        } else if (this.ID != null) {
            return "Flux n°" + this.ID;
        } else {
            return "FLUX ";
        }

//        String nomRetour = "";
//        if (this.getJournalLie() != null && this.getTypeFlux() != null) {
//            nomRetour += this.getJournalLie().getNom() + " - " + this.getTypeFlux().getDenomination();
//        } else {
//            nomRetour = this.getUrl();
//        }
//        return nomRetour;

    }

    public TacheRecupCallable getTacheRechupManuelle() {
        return tacheRechupManuelle;
    }

    public void setTacheRechupManuelle(TacheRecupCallable tacheRechupManuelle) {
        this.tacheRechupManuelle = tacheRechupManuelle;
    }

    void addItem(Item nouvellesItems) {
        this.item.add(nouvellesItems);
    }

    /**
     * *
     * Retourne un Opml du flux contenant ses sous flux
     *
     * @return
     */
    public Opml getOpml() {
        // On doit commencer par rechercher la liste des flux enfant
        DaoFlux daoFlux = DAOFactory.getInstance().getDAOFlux();
        Opml opml = new Opml();
        opml.setTitle(this.toString());

        Outline outline = this.getOpmlOutline();

        List<Outline> listoutline = new ArrayList<Outline>();
        listoutline.add(outline);

        opml.setOutlines(listoutline);

//        List<Flux> fluxs = daoFlux.findChildren(this.ID);
//
//        int i;
//        for (i = 0; i < fluxs.size(); i++) {
//            Outline suboutline = fluxs.get(i).getOpmlOutline();
//            
//        }
        return opml;
    }

    public Outline getOpmlOutline() {


        URL xmlUrl = null;
        try {
            xmlUrl = new URL(this.url);
        } catch (Exception e) {
        }

        URL outlinehtmlUrl = null;
        try {
            outlinehtmlUrl = new URL(this.htmlUrl);
        } catch (Exception e) {
        }


        Outline outline = new Outline(this.toString(), xmlUrl, outlinehtmlUrl);

        // On chercher les flux enfant;
        DaoFlux daoFlux = DAOFactory.getInstance().getDAOFlux();
        int i;
        List<Flux> fluxs = daoFlux.findChildren(this);
        for (i = 0; i < fluxs.size(); i++) {
            Outline subOutline = fluxs.get(i).getOpmlOutline();

            outline.getChildren().add(subOutline);
        }

        if (this.getTypeFlux() != null) {
            // On ajoute un attribut non conventionnelle pour préciser le type de flux 
            Attribute att = new Attribute("typeFlux", this.getTypeFlux().getDenomination());

            List<Attribute> listAtt = new ArrayList<Attribute>();
            listAtt.add(att);
            outline.getAttributes().add(att);

        }
        return outline;
    }

//    public void forceNotifyObserver() {
//        this.setChanged();
//        this.notifyObservers();
//    }
    @Override
    /**
     * *
     * Enregistre le flux auprès des service JMS et Service de collecte. Cette procédure ne peut être faite dans le
     * constructeur à cause de l'ORM
     */
    public void enregistrerAupresdesService() {
        this.deleteObservers();
        this.addObserver(ServiceCollecteur.getInstance());
//        this.addObserver(ServiceSynchro.getInstance());
    }

    @Override
    /**
     * *
     * Ce beans ne doit être synchronisé que si le booleen haschange (du partern observer/observable) est à true. On
     * évite ainsi de synchroniser après calcul des médiane décile et quartile qui ne change pas la nature du bean
     */
    public Boolean synchroImperative() {

        if (hasChanged()) {
            return true;
        }
        return false;
    }

    /**
     * *
     * Retourne la durée totale de captation du flux en s'appuyant sur les entites FluxPeriodeCaptation
     */
    public Long returnCaptationDuration() throws DonneeInterneCoherente {
        Long duration = new Long(0);

        int nbrPeriodeouverte = 0;

        for (int i = 0; i < periodeCaptations.size(); i++) {
            FluxPeriodeCaptation periode = this.periodeCaptations.get(i);
            if (periode.getDateDebut() != null & periode.getDatefin() != null) {
                DateTime dtDebut = new DateTime(periode.getDateDebut());
                DateTime dtFin = new DateTime(periode.getDatefin());
                Duration dur = new Duration(dtDebut, dtFin);
                duration += dur.getStandardSeconds();
            }
            if (periode.getDateDebut() != null && periode.getDatefin() == null) {
                DateTime dtDebut = new DateTime(periode.getDateDebut());
                DateTime dtFin = new DateTime(periode.getDatefin());
                Duration dur = new Duration(dtDebut, dtFin);
                duration += dur.getStandardSeconds();
                nbrPeriodeouverte++;
            }
        }
        if (nbrPeriodeouverte > 1) {
//            logger.error("Il y a deux période de captation ouverte pour le flux");
            throw new DonneeInterneCoherente("Il y a deux période de captation ouverte pour le flux");
        } else {
            return duration;
        }


//        return null;

    }

    /**
     * *
     * Cette méthode contient la logique métier permettant de gérer l'activation et la désactivation du flux. ajoute ou
     * modifie des enregistrements dans periodicité de collecte.
     *
     * @param activation Faut t'il activer ou désactiver le flux
     */
    public void activation(Boolean activation) {
//        if(activation){
//            this.periodeCaptations = new ArrayList<FluxPeriodeCaptation>();
//            FluxPeriodeCaptation fluxPeriodeCaptation = new FluxPeriodeCaptation();
//            this.periodeCaptations.add(fluxPeriodeCaptation);
//        }
        //Si activation
        if (activation) {
            //Si on a des période de captation
            if (!this.getPeriodeCaptations().isEmpty()) {

                FluxPeriodeCaptation lastperiode = this.getPeriodeCaptations().get(this.getPeriodeCaptations().size() - 1);
                // Si la date de din n'est pas null  on ajoute une période de captation
                if (lastperiode.getDatefin() != null) {
                    FluxPeriodeCaptation nPeriode = new FluxPeriodeCaptation();
                    nPeriode.setDateDebut(new Date());
                    nPeriode.setFlux(this);
                    nPeriode.setComportementDurantLaPeriode(mediatorFlux);
//                    System.out.println("LE mediator ICI 1 : " +);
                    this.getPeriodeCaptations().add(nPeriode);

                }
                // Si on n'a pas de période de captation
            } else if (this.getPeriodeCaptations().isEmpty()) {
                FluxPeriodeCaptation nP = new FluxPeriodeCaptation();
                nP.setDateDebut(new Date());
                nP.setFlux(this);
                nP.setComportementDurantLaPeriode(mediatorFlux);
                this.getPeriodeCaptations().add(nP);
            }
            // Si c'est une désactivation
        } else {
            // Si on a des période de captation
            if (!this.getPeriodeCaptations().isEmpty()) {
                // On récupère la derniere
                FluxPeriodeCaptation lastperiode = this.getPeriodeCaptations().get(this.getPeriodeCaptations().size() - 1);
                // On ferme la dernière période de captation si nécessaire
                if (lastperiode.getDatefin() == null) {
                    lastperiode.setDatefin(new Date());
                }
            }
        }
    }

    public static void main(String[] args) {
    }

    /**
     * *
     * Un changement de comportement entraine la création d'une nouvelle période de collecte. Cette méthode permet de
     * faire les modifications approprié au niveau métier
     *
     * @param compoNouv
     */
    public void changerComportement(MediatorCollecteAction compoNouv) {
        try {
            activation(false);
            activation(true);

            int lastIndex = 0;
            if (this.periodeCaptations.size() > 0) {
                lastIndex = this.periodeCaptations.size() - 1;
            }

            this.mediatorFlux = compoNouv;
            this.periodeCaptations.get(lastIndex).setComportementDurantLaPeriode(compoNouv);
        } catch (Exception e) {
            System.out.println(">>>>>>>>>>>>>>>>>>><ERRR" + e);
        }
        System.out.println(">>>>>>>>>>>>>>>>PAS D'ERREUR");


//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}