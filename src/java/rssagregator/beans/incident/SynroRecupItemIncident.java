/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.beans.incident;

import javax.persistence.Entity;
import rssagregator.services.ServiceSynchro;
import rssagregator.services.tache.TacheSynchroRecupItem;

/**
 * <strong>/!\ N'est plus utilisé. La synchronisation est retirée des objectifs du projet</strong>
 *  Incident survenu lors de la récupération des item tache {@link TacheSynchroRecupItem}. L'incident est généré par le service {@link ServiceSynchro} 
 * @author clem
 * @deprecated 
 */
@Entity(name = "i_RecupItemIncident")
public class SynroRecupItemIncident extends AbstrIncident{
    
}
