package rssagregator.beans.incident;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import rssagregator.beans.Flux;

/** 
 *  Les erreurs de captation sont consigné dans des objets redéfinissant cette classe abstraite. Il peut s'agir d'erreur de parsage, d'erreur http (404, site indisponible etc...)
 */
@Entity(name = "incidentflux")
public class FluxIncident extends AbstrIncident implements Serializable  {

    
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    private Long ID;
//
//    public FluxIncident() {
//    }
    

    

    
   

    
    
  /** 
   *  Un objet flux peut posséder différents incidents. Un incident ne possède qu'un flux. 
   */
    
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Flux fluxLie;
      
    
    
 
//  private Flux flux;

    public Flux getFluxLie() {
        return fluxLie;
    }

    public void setFluxLie(Flux fluxLie) {
        this.fluxLie = fluxLie;
    }

//    public Long getID() {
//        return ID;
//    }
//
//    public void setID(Long ID) {
//        this.ID = ID;
//    }

}