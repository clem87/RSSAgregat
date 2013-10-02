/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.beans.traitement;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import rssagregator.dao.DAOFactory;
import rssagregator.dao.DaoItem;
import java.util.List;
import java.util.logging.Level;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.buf.HexUtils;
import rssagregator.beans.Flux;
import rssagregator.beans.Item;

/**
 *
 * @author clem
 */
//@Entity()
@Entity
@Table(name = "tr_dedoub")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstrDedoublonneur implements Serializable, Cloneable {

    public AbstrDedoublonneur() {
        // On initialise le tableau de compte capture
        compteCapture = new Integer[6];
        compteCapture[0] = 0;
        compteCapture[1] = 0;
        compteCapture[2] = 0;
        compteCapture[3] = 0;
        compteCapture[4] = 0;
    }
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Long ID;
    @Transient
    protected Logger logger = Logger.getLogger(AbstrDedoublonneur.class);
    @Column(name = "deboubTitle")
    protected Boolean deboubTitle;
    @Column(name = "dedoubLink")
    protected Boolean dedoubLink;
    @Column(name = "deboudDesc")
    protected Boolean deboudDesc;
    @Column(name = "dedoubDatePub")
    protected Boolean dedoubDatePub;
    @Column(name = "dedouGUID")
    protected Boolean dedouGUID;
    @Column(name = "dedoubCategory")
    protected Boolean dedoubCategory;
    /**
     * *
     * 1=nombre item trouvé ; 2 dedoub memoire; 3 BDD item lié ;4 BDD item déjà présente mais lien ajouté ; 5 item
     * nouvelles
     */
    @Transient
    protected Integer[] compteCapture;

    /**
     * *
     * Test si l'on a déjà enregitré l'item.
     *
     * @param item
     * @param flux
     * @return True si l'item à déjà été enregistrée. False si l'item est nouvelle et n'a pas encoe été taité
     */
//    @Deprecated
//    public Boolean testDoublonageMemoire(Item item, Flux flux) {
//        int i = 0;
//        // test basé sur les dernières emprunte en mémoire.
//        while (i < flux.getLastEmpruntes().size()) {
//            if (item.getHashContenu().equals(flux.getLastEmpruntes().get(i))) {
//                return Boolean.TRUE;
//            }
//            i++;
//        }
//        return false;
//    }

    @Deprecated
    public Boolean testDoublonageBDD(Item get, Flux flux) {
        // On test si on peu trouver une item possédant le hash pour le flux
        DaoItem dao = DAOFactory.getInstance().getDaoItem();
//        dao.findHashFlux(get.getHashContenu(), flux);


        return false;
    }

   public abstract List<Item> dedoublonne(List<Item> listItemCapture, Flux flux);

    public Boolean getDeboubTitle() {
        return deboubTitle;
    }

    public void setDeboubTitle(Boolean deboubTitle) {
        this.deboubTitle = deboubTitle;
    }

    public Boolean getDedoubLink() {
        return dedoubLink;
    }

    public void setDedoubLink(Boolean dedoubLink) {
        this.dedoubLink = dedoubLink;
    }

    public Boolean getDeboudDesc() {
        return deboudDesc;
    }

    public void setDeboudDesc(Boolean deboudDesc) {
        this.deboudDesc = deboudDesc;
    }

    public Boolean getDedoubDatePub() {
        return dedoubDatePub;
    }

    public void setDedoubDatePub(Boolean dedoubDatePub) {
        this.dedoubDatePub = dedoubDatePub;
    }

    public Boolean getDedouGUID() {
        return dedouGUID;
    }

    public void setDedouGUID(Boolean dedouGUID) {
        this.dedouGUID = dedouGUID;
    }

    public Boolean getDedoubCategory() {
        return dedoubCategory;
    }

    public void setDedoubCategory(Boolean dedoubCategory) {
        this.dedoubCategory = dedoubCategory;
    }

    public Integer[] getCompteCapture() {
        return compteCapture;
    }

    public void setCompteCapture(Integer[] compteCapture) {
        this.compteCapture = compteCapture;
    }

    /**
     * *
     * Calcul et retourne le hash pour l'item envoyé en argument
     *
     * @param it L'item pour laquelle le hash doit être calculé
     * @return une chaine de caractère comprenant le hash Md5
     */
    public String returnHash(Item it) throws NoSuchAlgorithmException {

        String concat = "";

        if (this.deboubTitle && it.getTitre() != null) {
            concat += it.getTitre();
        }

        if (this.deboudDesc && it.getDescription() != null) {
            concat += it.getDescription();
        }

        if (this.dedouGUID && it.getDescription() != null) {
            concat += it.getGuid();
        }

        if (this.dedoubLink && it.getLink() != null) {
            concat += it.getLink();
        }

        if (this.dedoubDatePub && it.getDatePub() != null) {
            concat += it.getDatePub().toString();
        }

        if (this.dedoubCategory && it.getCategorie() != null) {
            concat += it.getCategorie();
        }

//                concat = item.getTitre() + item.getDescription();
        MessageDigest digest;
     
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] hash = digest.digest(concat.getBytes());
            String hashString = new String(HexUtils.toHexString(hash));
            return hashString;

    }

    /**
     * *
     * Calcul les hash pour la list des items envoyés en paramètre
     *
     * @param listItem
     */
    protected void calculHash(List<Item> listItem) throws NoSuchAlgorithmException {
        int i;

        for (i = 0; i < listItem.size(); i++) {
            String concat = "";
            Item item = listItem.get(i);

            if (this.deboubTitle && item.getTitre() != null) {
                concat += item.getTitre();
            }

            if (this.deboudDesc && item.getDescription() != null) {
                concat += item.getDescription();
            }

            if (this.dedouGUID && item.getDescription() != null) {
                concat += item.getGuid();
            }

            if (this.dedoubLink && item.getLink() != null) {
                concat += item.getLink();
            }

            if (this.dedoubDatePub && item.getDatePub() != null) {
                concat += item.getDatePub().toString();
            }

            if (this.dedoubCategory && item.getCategorie() != null) {
                concat += item.getCategorie();
            }

//                concat = item.getTitre() + item.getDescription();
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] hash = digest.digest(concat.getBytes());
            String hashString = new String(HexUtils.toHexString(hash));
            item.setHashContenu(hashString);
        }
    }

    public Long getID() {
        return ID;
    }

    public void setID(Long ID) {
        this.ID = ID;
    }

    @Override
    protected AbstrDedoublonneur clone() throws CloneNotSupportedException {
        AbstrDedoublonneur clone = (AbstrDedoublonneur) super.clone();
        clone.compteCapture = this.compteCapture.clone();

        return clone; //To change body of generated methods, choose Tools | Templates.
    }
}