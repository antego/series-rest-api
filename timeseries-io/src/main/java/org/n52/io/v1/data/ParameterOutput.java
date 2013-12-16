/**
 * ﻿Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.io.v1.data;

import java.text.Collator;

public class ParameterOutput implements CollatorComparable<ParameterOutput> {

    private String id;

    private String label;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the label or the id if label is not set.
     */
    public String getLabel() {
        // ensure that label is never null
        return label == null ? id : label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    
    public int compare(Collator collator, ParameterOutput o) {
        if (collator == null) {
            collator = Collator.getInstance();
        }
        return collator.compare(getLabel().toLowerCase(), o.getLabel().toLowerCase());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (id == null) ? 0 : id.hashCode());
        result = prime * result + ( (label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof ParameterOutput)) {
            return false;
        }
        ParameterOutput other = (ParameterOutput) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        }
        else if ( !id.equals(other.id)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        }
        else if ( !label.equals(other.label)) {
            return false;
        }
        return true;
    }

}