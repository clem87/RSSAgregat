/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.services.tache;

import java.util.List;

/**
 * 
 * /!\ N'est pas fonctionnel. On a retirer la synchronisation du projet
 * Cette tache est lancée toutes les semaines afin de récupérérer sur les serveurs esclaves les items collectées qui
 * manqueraient sur le serveur maitre
 *
 * @author clem
 */
@Deprecated
public class TacheSynchroHebdomadaire extends TacheImpl<TacheSynchroHebdomadaire> {

//    public TacheSynchroHebdomadaire(Observer s) {
//        super(s);
//        erreur = false;
//    }

    public TacheSynchroHebdomadaire() {
        super();
    }
    /**
     * *
     * Un flag utilisé dans le call;
     */
    private Boolean erreur;
    private List<ZZOLDTacheSynchroRecupItem> synchroSlave;

    @Override
    protected void callCorps() throws Exception {
//        synchroSlave = new ArrayList<TacheSynchroRecupItem>();
//        // Pour chaque serveur slave
//        List<ServeurSlave> listSlave = DAOFactory.getInstance().getDAOConf().getConfCourante().getServeurSlave(); // Pour chaque serveur esclave
//        for (int i = 0; i < listSlave.size(); i++) {
//            ServeurSlave serveurSlave = listSlave.get(i);
//            ZZOLDTacheSynchroRecupItem t = new ZZOLDTacheSynchroRecupItem(ServiceSynchro.getInstance());
//            t.setServeurSlave(serveurSlave);
//            synchroSlave.add(t);
//            Future<TacheSynchroRecupItem> futur = ServiceSynchro.getInstance().getExecutorService().submit(t);
//            ZZOLDTacheSynchroRecupItem recupItem = futur.get();
//
//            if (recupItem.getExeption() != null) {
//                erreur = true;
//            }
//        }
    }

//    @Override
//    public TacheSynchroHebdomadaire call() throws Exception {
//        this.exeption = null;
//        try {
//            synchroSlave = new ArrayList<TacheSynchroRecupItem>();
//
//
//            // Pour chaque serveur slave
//            List<ServeurSlave> listSlave = DAOFactory.getInstance().getDAOConf().getConfCourante().getServeurSlave(); // Pour chaque serveur esclave
//            for (int i = 0; i < listSlave.size(); i++) {
//                ServeurSlave serveurSlave = listSlave.get(i);
//                ZZOLDTacheSynchroRecupItem t = new ZZOLDTacheSynchroRecupItem(ServiceSynchro.getInstance());
//                t.setServeurSlave(serveurSlave);
//                synchroSlave.add(t);
//                Future<TacheSynchroRecupItem> futur = ServiceSynchro.getInstance().getExecutorService().submit(t);
//                ZZOLDTacheSynchroRecupItem recupItem = futur.get();
//
//                if (recupItem.getExeption() != null) {
//                    erreur = true;
//                }
//            }
//
//        } catch (Exception e) {
//        } finally {
//            if (erreur) {
//                this.exeption = new Exception("Des erreurs se sont produitent lors de la Synchronisation hebdomadaire");
//            }
//            return this;
//        }
//
////        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    public Boolean getErreur() {
        return erreur;
    }

    public void setErreur(Boolean erreur) {
        this.erreur = erreur;
    }

    public List<ZZOLDTacheSynchroRecupItem> getSynchroSlave() {
        return synchroSlave;
    }

    public void setSynchroSlave(List<ZZOLDTacheSynchroRecupItem> synchroSlave) {
        this.synchroSlave = synchroSlave;
    }
}
