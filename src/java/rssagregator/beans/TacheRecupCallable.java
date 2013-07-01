/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.beans;

import rssagregator.dao.DAOFactory;
import rssagregator.dao.DaoItem;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import rssagregator.beans.incident.AbstrIncident;
import rssagregator.services.ServiceCollecteur;

/**
 *
 * @author clem
 */
public class TacheRecupCallable extends Observable implements Callable<List<Item>> {

    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TacheRecupCallable.class);
    /**
     * *
     * Les items capturées par la tache
     */
    List<Item> nouvellesItems;
    /**
     * *
     * Le flux de la tache
     */
    public Flux flux;
    /**
     * A chaque récupération, il faut inscrire dans cette variable la date.
     * Cette variable est ainsi modifié au lancement de la méthode run
     */
    public Date DateDerniereRecup;
    /**
     * *
     * Lorsqu'une exeption survient, on stocke sa référence ici
     */
    public AbstrIncident incident;
    Boolean tacheSchedule;
    Boolean persit;

    public TacheRecupCallable(Flux flux) {
        this.flux = flux;
        this.addObserver(flux);
        incident = null;
        tacheSchedule = false;
        persit = true;
    }

    @Override
    public List<Item> call() throws Exception {
        // On block le flux pour eviter que la tache automanique et la tache manuelle agissent en même temps
        synchronized (this.flux) {

            logger.debug("Recup du flux : " + this.flux.getID() + ". " + flux);


            flux.setTacheRechup(this);

            nouvellesItems = this.flux.getMediatorFlux().executeActions(this.flux);
//                   logger.info("Recup du flux : " + this.flux.getID()+". "+flux+" Nombre de nouvelles item : " + nouvellesItems.size());


//            System.out.println("###############################################################");
//            System.out.println("Lancement de la tache : " + flux.getUrl());
//            System.out.println("Nombre d'item rapporté pa le médiatoAction (nouvelles ou a lier) : " + nouvellesItems.size());
//            System.out.println("###############################################################");
            // On enregistre ces nouvelles items

            DaoItem daoItem = DAOFactory.getInstance().getDaoItem();

            int i;
            for (i = 0; i < nouvellesItems.size(); i++) {
                // 
                nouvellesItems.get(i).getListFlux().add(flux);
//                daoItem.creer(nouvellesItems.get(i));
                this.flux.addItem(nouvellesItems.get(i));
//                DAOFactory.getInstance().getEntityManager().refresh(flux);

//                this.flux.getItem().add(nouvellesItems.get(i));
                this.flux.getLastEmpruntes().add(nouvellesItems.get(i).getHashContenu());
            }

            // On supprime des hash pour éviter l'accumulation. On en laisse 20 en plus du nombre d'item contenues dans le flux.

            Integer nbr = flux.getMediatorFlux().getNbrItemCollecte() + 19;
            if (nbr > 0 && nbr < flux.getLastEmpruntes().size()) {
                for (i = nbr; i < flux.getLastEmpruntes().size(); i++) {
                    flux.getLastEmpruntes().remove(i);

//                    System.out.println("TACHE RECUP : SUPPRESSION D'UN HASH");
                }
            }

            flux.fermerLesIncidentOuvert();

            DebugRecapLeveeFlux debug = new DebugRecapLeveeFlux();
            debug.setDate(new Date());
            debug.setNbrRecup(nouvellesItems.size());
            flux.getDebug().add(debug);



            // TODO : On peut utiliser le pattern observer pour notifier au flux des nouveautés. Il faut en effet enregistrer les incidents
            if (persit) {
                try {
                    DAOFactory.getInstance().getDAOFlux().modifierFlux(flux);
                } catch (Exception e) {
                }

            }

//                ListeFluxCollecteEtConfigConrante.getInstance().modifierFlux(flux);
//            logger.info("Tache RECUP : NBR item Collecté après dédoublonage : ");
//            System.out.println("Tache RECUP : NBR item Collecté après dédoublonage : " + flux.getItem().size());


            // Si il s'agit d'une tache schedule, il faut la réajouter au scheduler
            if (tacheSchedule) {
                ServiceCollecteur.getInstance().addScheduledCallable(this);
            }

            // On supprimer les items capturée du cache de l'ORM pour éviter l'encombrement

            for (i = 0; i < nouvellesItems.size(); i++) {
                DAOFactory.getInstance().getEntityManager().detach(nouvellesItems.get(i));
            }



            logger.info("Flux : " + this.flux.getID() + ". (" + flux + "). Nbr item recup : " + nouvellesItems.size());
            return nouvellesItems;
        }

    }

    public List<Item> getNouvellesItems() {
        return nouvellesItems;
    }

    public void setNouvellesItems(List<Item> nouvellesItems) {
        this.nouvellesItems = nouvellesItems;
    }

    public Flux getFlux() {
        return flux;
    }

    public void setFlux(Flux flux) {
        this.flux = flux;
    }

    public Date getDateDerniereRecup() {
        return DateDerniereRecup;
    }

    public void setDateDerniereRecup(Date DateDerniereRecup) {
        this.DateDerniereRecup = DateDerniereRecup;
    }

    public AbstrIncident getIncident() {
        return incident;
    }

    public void setIncident(AbstrIncident incident) {
        this.incident = incident;
    }

    public static void main(String[] args) {
        Flux f = new Flux();
        f.setUrl("http://rss.lemkonde.fr/c/205/f/3050/index.rss");


        TacheRecupCallable t = new TacheRecupCallable(f);
        try {
            t.call();
        } catch (Exception ex) {
            System.out.println("TACHERECUPCALLABLE : CAPTURE D'UNE EXEPTION");
            Logger.getLogger(TacheRecupCallable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean getTacheSchedule() {
        return tacheSchedule;
    }

    public void setTacheSchedule(Boolean tacheSchedule) {
        this.tacheSchedule = tacheSchedule;
    }

    public Boolean getPersit() {
        return persit;
    }

    public void setPersit(Boolean persit) {
        this.persit = persit;
    }
}
