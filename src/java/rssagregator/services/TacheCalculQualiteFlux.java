/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.services;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import javax.persistence.LockModeType;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import rssagregator.beans.Flux;
import rssagregator.beans.FluxPeriodeCaptation;
import rssagregator.beans.Item;
import rssagregator.beans.POJOCompteItem;
import rssagregator.beans.incident.AnomalieCollecte;
import rssagregator.beans.incident.CollecteIncident;
import rssagregator.beans.incident.IncidentFactory;
import rssagregator.dao.DAOFactory;
import rssagregator.dao.DAOIncident;
import rssagregator.dao.DaoFlux;
import rssagregator.dao.DaoItem;
import rssagregator.services.crud.ServiceCRUDFactory;
import rssagregator.services.crud.ServiceCrudIncident;

/**
 * Cette tâche effectue différents calculs observant le nombre d'item /jour capturés par un flux. Les calculs sont
 * effectué vis à vis de la dernière {@link FluxPeriodeCaptation} du flux. Cette tache permet de calculer les données
 * permettant d'afficher une box ploat (min quartile mediane décile max). Elle calcul aussi l'indice de qualité de
 * captation.
 *
 *
 * @author clem
 */
public class TacheCalculQualiteFlux extends TacheImpl<TacheCalculQualiteFlux> {

    private Flux flux;
    Float indiceCaptation;
    Integer mediane;
    Integer decile;
    Integer quartile;
    Integer maximum;
    Integer minimum;
    protected org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TacheCalculQualiteFlux.class);

    public TacheCalculQualiteFlux(Observer s) {
        super(s);
    }

    public TacheCalculQualiteFlux() {
        super();
    }

//    @Override
//    public TacheCalculQualiteFlux call() throws DonneeInterneCoherente, Exception {
//        DaoFlux daof = DAOFactory.getInstance().getDAOFlux();
//        DaoItem daoItem = DAOFactory.getInstance().getDaoItem();
//        EntityManager em = daof.getEm();
//        daoItem.setEm(em);
//        em.getTransaction().begin();
//
//        try {
//
//            this.exeption = null;
//
//            if (!em.contains(flux)) { // On bloque la ressouce flux car elle va être modifié et persité
//                Flux f = em.find(Flux.class, flux.getID());
//                em.lock(f, LockModeType.PESSIMISTIC_WRITE);
//            } else {
//                em.lock(flux, LockModeType.PESSIMISTIC_WRITE);
//            }
//
//
//
//
//
//            //------------------------------CALCUL DE L'incide de captation
//            Long nbrSecondCaptation = flux.returnCaptationDuration();
//            // On doit ensuite chercher tous les incident de collecte lié à ce flux. On obtient un nombre d'heure. 
//            List<CollecteIncident> listIncid = flux.getIncidentsLie();
//            Long timeIncid = new Long(0);
//            for (int i = 0; i < listIncid.size(); i++) {
//                CollecteIncident collecteIncident = listIncid.get(i);
//
//                // Certaines exeption ne doivent pas être relevée dans le calcul.
//                if (collecteIncident.getClass().equals(AnomalieCollecte.class)) {
//                    AnomalieCollecte cast = (AnomalieCollecte) collecteIncident;
//                    if (cast.getCauseChangementLigneEditoriale() != null && cast.getCauseChangementLigneEditoriale()) { // Si l'anomalie est due a un changement de ligne éditoriale
//                        continue;
//                    }
//                }
//
//                if (collecteIncident.getDateDebut() != null && collecteIncident.getDateFin() == null) {
//                    DateTime dtDebut = new DateTime(collecteIncident.getDateDebut());
//                    DateTime dtFin = new DateTime(collecteIncident.getDateFin());
//                    Duration dur = new Duration(dtDebut, dtFin);
//                    timeIncid += dur.getStandardSeconds();
//                } else if (collecteIncident.getDateDebut() != null && collecteIncident.getDateFin() == null) {
//                    DateTime dtDebut = new DateTime(collecteIncident.getDateDebut());
//                    DateTime dtFin = new DateTime();
//                    Duration dur = new Duration(dtDebut, dtFin);
//                    timeIncid += dur.getStandardSeconds();
//                }
//            }
//            if (timeIncid != 0) {
//                Float indice = (100 - (timeIncid.floatValue() / nbrSecondCaptation.floatValue()));
//                flux.setIndiceQualiteCaptation(indice);
////            flux.setIndiceQualiteCaptation(indice.intValue());
//            } else {
//                flux.setIndiceQualiteCaptation(new Float(100.00));
//            }
//
//
//
//            //-----------------Calcul de la moyenne médiane quartiele décile....
//
//            FluxPeriodeCaptation period = flux.returnDerniereFluxPeriodeCaptation();
//            Date date1 = period.getDateDebut();
//            Date date2 = new DateTime(period.getDatefin()).minusDays(1).withEarlierOffsetAtOverlap().toDate(); // Pour la date 2, on prend la date de fin de période - 1 jour avec heures la plus tardive
//
//
//            List<Item> items = daoItem.itemCaptureParleFluxDurantlaDernierePeriodeCollecte(flux);
//            POJOCompteItem compteItem = new POJOCompteItem();
//            compteItem.setFlux(flux);
//            compteItem.setItems(items);
//            compteItem.setDate1(date1);
//            compteItem.setDate2(date2);
//            compteItem.compte();
//            compteItem.calculterBoxPloat();
//
//            flux.setIndiceMaximumNbrItemJour(compteItem.getMax().intValue());
//            flux.setIndiceMinimumNbrItemJour(compteItem.getMin());
//            flux.setIndiceQuartileNbrItemJour(compteItem.getQuartile());
//            flux.setIndiceDecileNbrItemJour(compteItem.getDecile());
//            flux.setIndiceMedianeNbrItemJour(compteItem.getMediane());
//
//
//
//
//            //--------------Detection des anomalies 
//            compteItem.calculerMoyenne(date1, date2);
//            Map<Date, Integer> anomalies = compteItem.detecterAnomalieParrapportAuSeuil(33);
//
//            IncidentFactory<AnomalieCollecte> factory = new IncidentFactory<AnomalieCollecte>();
//            ServiceCrudIncident serviceCrud = (ServiceCrudIncident) ServiceCRUDFactory.getInstance().getServiceFor(AnomalieCollecte.class);
//
//            DAOIncident<AnomalieCollecte> daoIncident = (DAOIncident<AnomalieCollecte>) DAOFactory.getInstance().getDaoFromType(AnomalieCollecte.class);
//
//            List<AnomalieCollecte> AnomalieDsBSS = daoIncident.findOpenCollecteIncident(flux, AnomalieCollecte.class, null);
//            
//
//            for (Map.Entry<Date, Integer> entry : anomalies.entrySet()) {
//                Date date = entry.getKey();
//                Integer nbtConstate = entry.getValue();
//                AnomalieCollecte anomalieIncident = factory.getIncident(AnomalieCollecte.class, "Le nombre d'item capturé pour ce jour est anormalement haut ou bas", null);
//                anomalieIncident.setDateDebut(date);
//                anomalieIncident.setNombreCaptureConstate(nbtConstate);
//                anomalieIncident.setFluxLie(flux);
//                anomalieIncident.setMoyenneDesCapture(compteItem.getMoyenne());
//                anomalieIncident.setSeuil(33);
//
//                serviceCrud.ajouterIncidentdeCollecte(anomalieIncident, flux, em, false);
//            }
//
//
//
//            //------------Enregistrement
//            em.merge(flux); // On modifi le flux sans passer par le service, on ne veut pas que ces chiffre donnenen tlieu à une synchronisation
//        } catch (Exception e) {
//            this.exeption = e;
//            if (em != null && em.isJoinedToTransaction()) {
//                em.getTransaction().rollback();
//            }
//        } finally {
//            if (em != null && em.isJoinedToTransaction()) {
//                em.getTransaction().commit();
//            }
//
//            this.setChanged();
//            this.notifyObservers();
//            return this;
//        }
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    public Flux getFlux() {
        return flux;
    }

    public void setFlux(Flux flux) {
        this.flux = flux;
    }

    public Float getIndiceCaptation() {
        return indiceCaptation;
    }

    public void setIndiceCaptation(Float indiceCaptation) {
        this.indiceCaptation = indiceCaptation;
    }

    public Integer getMediane() {
        return mediane;
    }

    public void setMediane(Integer mediane) {
        this.mediane = mediane;
    }

    public Integer getDecile() {
        return decile;
    }

    public void setDecile(Integer decile) {
        this.decile = decile;
    }

    public Integer getQuartile() {
        return quartile;
    }

    public void setQuartile(Integer quartile) {
        this.quartile = quartile;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

//    public class CompteItemJour {
//
//        Date date;
//        Integer compte;
//
//        public CompteItemJour(Date date, Integer compte) {
//            this.date = date;
//            this.compte = compte;
//        }
//
//        public Date getDate() {
//            return date;
//        }
//
//        public void setDate(Date date) {
//            this.date = date;
//        }
//
//        public Integer getCompte() {
//            return compte;
//        }
//
//        public void setCompte(Integer compte) {
//            this.compte = compte;
//        }
//    }
    @Override
    protected void callCorps() throws Exception {
        logger.debug("Calcul calité flux " + flux);
        em = DAOFactory.getInstance().getEntityManager();
        em.getTransaction().begin();
        verrouillerObjectDansLEM(flux, LockModeType.PESSIMISTIC_WRITE);
        DaoFlux daof = DAOFactory.getInstance().getDAOFlux();
        DaoItem daoItem = DAOFactory.getInstance().getDaoItem();
        daoItem.setEm(em);







        //------------------------------CALCUL DE L'incide de captation
        Long nbrSecondCaptation = flux.returnCaptationDuration();
        // On doit ensuite chercher tous les incident de collecte lié à ce flux. On obtient un nombre d'heure. 
        List<CollecteIncident> listIncid = flux.getIncidentsLie();
        Long timeIncid = new Long(0);
        for (int i = 0; i < listIncid.size(); i++) {
            CollecteIncident collecteIncident = listIncid.get(i);

            // Certaines exeption ne doivent pas être relevée dans le calcul.
            if (collecteIncident.getClass().equals(AnomalieCollecte.class)) {
                AnomalieCollecte cast = (AnomalieCollecte) collecteIncident;
                if (cast.getCauseChangementLigneEditoriale() != null && cast.getCauseChangementLigneEditoriale()) { // Si l'anomalie est due a un changement de ligne éditoriale
                    continue;
                }
            }

            if (collecteIncident.getDateDebut() != null && collecteIncident.getDateFin() == null) {
                DateTime dtDebut = new DateTime(collecteIncident.getDateDebut());
                DateTime dtFin = new DateTime(collecteIncident.getDateFin());
                Duration dur = new Duration(dtDebut, dtFin);
                timeIncid += dur.getStandardSeconds();
            } else if (collecteIncident.getDateDebut() != null && collecteIncident.getDateFin() == null) {
                DateTime dtDebut = new DateTime(collecteIncident.getDateDebut());
                DateTime dtFin = new DateTime();
                Duration dur = new Duration(dtDebut, dtFin);
                timeIncid += dur.getStandardSeconds();
            }
        }
        if (timeIncid != 0) {
            Float indice = (100 - (timeIncid.floatValue() / nbrSecondCaptation.floatValue()));
            flux.setIndiceQualiteCaptation(indice);
//            flux.setIndiceQualiteCaptation(indice.intValue());
        } else {
            flux.setIndiceQualiteCaptation(new Float(100.00));
        }



        //-----------------Calcul de la moyenne médiane quartiele décile....

        FluxPeriodeCaptation period = flux.returnDerniereFluxPeriodeCaptation();
        Date date1 = period.getDateDebut();
        Date date2 = null;
        if(period.getDatefin()!=null){
        date2 = new DateTime(period.getDatefin()).minusDays(1).withEarlierOffsetAtOverlap().toDate(); // Pour la date 2, on prend la date de fin de période - 1 jour avec heures la plus tardive    
        }
        else{
            date2 = new Date();
        }
        


        List<Item> items = daoItem.itemCaptureParleFluxDurantlaDernierePeriodeCollecte(flux);
        POJOCompteItem compteItem = new POJOCompteItem();
        compteItem.setFlux(flux);
        compteItem.setItems(items);
        compteItem.setDate1(date1);
        compteItem.setDate2(date2);
        compteItem.compte();
        compteItem.calculterBoxPloat();

        flux.setIndiceMaximumNbrItemJour(compteItem.getMax().intValue());
        flux.setIndiceMinimumNbrItemJour(compteItem.getMin());
        flux.setIndiceQuartileNbrItemJour(compteItem.getQuartilePremier());
        flux.setIndiceDecileNbrItemJour(compteItem.getQuartileTrois());
        flux.setIndiceMedianeNbrItemJour(compteItem.getMediane());
        
        period.setStatMax(compteItem.getMax());
        period.setStatMin(compteItem.getMin());
        period.setStatQuartilePremier(compteItem.getQuartilePremier());
        period.setStatQuartileTrois(compteItem.getQuartileTrois());
        period.setStatMedian(compteItem.getMediane());
        period.setStatMoyenne(compteItem.getMoyenne());
        period.setStatEcartType(compteItem.getEcartType());
        period.setStatSommeItemCapture(compteItem.getSomme());

        // Stat par jour
//        for (int i = 1; i < 8; i++) {
//            period.sets
//            
//        }
        period.setStatEcartTypeLundi(compteItem.getStatEcartypeDayOfWeek()[0]);
        period.setStatMedLundi(compteItem.getStatMoyDayOfWeek()[0]);
        period.setStatMoyLundi(compteItem.getStatMoyDayOfWeek()[0]);

        period.setStatEcartTypeMardi(compteItem.getStatEcartypeDayOfWeek()[1]);
        period.setStatMedMardi(compteItem.getStatMoyDayOfWeek()[1]);
        period.setStatMoyMardi(compteItem.getStatMoyDayOfWeek()[1]);

        period.setStatEcartTypeMercredi(compteItem.getStatEcartypeDayOfWeek()[2]);
        period.setStatMedMercredi(compteItem.getStatMoyDayOfWeek()[2]);
        period.setStatMoyMercredi(compteItem.getStatMoyDayOfWeek()[2]);

        period.setStatEcartTypeJeudi(compteItem.getStatEcartypeDayOfWeek()[3]);
        period.setStatMedJeudi(compteItem.getStatMoyDayOfWeek()[3]);
        period.setStatMoyJeudi(compteItem.getStatMoyDayOfWeek()[3]);

        period.setStatEcartTypeVendredi(compteItem.getStatEcartypeDayOfWeek()[4]);
        period.setStatMedVendredi(compteItem.getStatMoyDayOfWeek()[4]);
        period.setStatMoyVendredi(compteItem.getStatMoyDayOfWeek()[4]);

        period.setStatEcartTypeSamedi(compteItem.getStatEcartypeDayOfWeek()[5]);
        period.setStatMedSamedi(compteItem.getStatMoyDayOfWeek()[5]);
        period.setStatMoySamedi(compteItem.getStatMoyDayOfWeek()[5]);

        period.setStatEcartTypeDimanche(compteItem.getStatEcartypeDayOfWeek()[6]);
        period.setStatMedDimanche(compteItem.getStatMoyDayOfWeek()[6]);
        period.setStatMoyDimanche(compteItem.getStatMoyDayOfWeek()[6]);




        //--------------Detection des anomalies 
        compteItem.calculerMoyenne(date1, date2);
        Map<Date, Integer> anomalies = compteItem.detecterAnomalieParrapportAuSeuil(33);

        IncidentFactory<AnomalieCollecte> factory = new IncidentFactory<AnomalieCollecte>();
        ServiceCrudIncident serviceCrud = (ServiceCrudIncident) ServiceCRUDFactory.getInstance().getServiceFor(AnomalieCollecte.class);

        DAOIncident<AnomalieCollecte> daoIncident = (DAOIncident<AnomalieCollecte>) DAOFactory.getInstance().getDaoFromType(AnomalieCollecte.class);

        List<AnomalieCollecte> AnomalieDsBSS = daoIncident.findOpenCollecteIncident(flux, AnomalieCollecte.class, null);


        for (Map.Entry<Date, Integer> entry : anomalies.entrySet()) {
            Date date = entry.getKey();
            Integer nbtConstate = entry.getValue();
            AnomalieCollecte anomalieIncident = factory.getIncident(AnomalieCollecte.class, "Le nombre d'item capturé pour ce jour est anormalement haut ou bas", null);
            anomalieIncident.setDateDebut(date);
            anomalieIncident.setNombreCaptureConstate(nbtConstate);
            anomalieIncident.setFluxLie(flux);
            anomalieIncident.setMoyenneDesCapture(compteItem.getMoyenne());
            anomalieIncident.setSeuil(33);

            serviceCrud.ajouterIncidentdeCollecte(anomalieIncident, flux, em, false);
        }



        //------------Enregistrement
        em.merge(flux); // On modifi le flux sans passer par le service, on ne veut pas que ces chiffre donnenen tlieu à une synchronisation

    }
}
