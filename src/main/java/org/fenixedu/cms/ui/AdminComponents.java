/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu CMS.
 *
 * FenixEdu CMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu CMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu CMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.cms.ui;

import static pt.ist.fenixframework.FenixFramework.atomic;

import org.fenixedu.bennu.spring.portal.BennuSpringController;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.CMSComponent;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.ComponentDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

@BennuSpringController(AdminSites.class)
@RequestMapping("/cms/components")
public class AdminComponents {

    @RequestMapping(value = "{slugSite}/{slugPage}/createComponent", method = RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<String> createComponent(@PathVariable(value = "slugSite") String slugSite, @PathVariable(
            value = "slugPage") String slugPage, @RequestBody String json) throws JsonSyntaxException, Exception {
        Site site = Site.fromSlug(slugSite);
        AdminSites.canEdit(site);

        Page page = site.pageForSlug(slugPage);

        createComponent(site, page, new JsonParser().parse(json).getAsJsonObject());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Atomic(mode = TxMode.WRITE)
    private void createComponent(Site site, Page page, JsonObject json) throws Exception {
        String componentType = json.get("type").getAsString();
        ComponentDescriptor descriptor = Component.forType(componentType);
        if (descriptor == null) {
            throw new IllegalArgumentException("Component '" + componentType + "' is unknown!");
        }
        if (descriptor.isStateless()) {
            @SuppressWarnings("unchecked")
            Class<? extends CMSComponent> type = (Class<? extends CMSComponent>) descriptor.getType();
            page.addComponents(Component.forType(type));
        } else {
            JsonObject params = json.get("parameters").getAsJsonObject();
            page.addComponents(descriptor.instantiate(params));
        }
    }

    @ResponseBody
    @RequestMapping(value = "/componentArguments/{page}", produces = "application/json;charset=UTF-8")
    public String getComponentArguments(@PathVariable("page") Page page, @RequestParam("type") String type) {
        AdminSites.canEdit(page.getSite());
        ComponentDescriptor descriptor = Component.forType(type);
        if (descriptor == null) {
            throw new IllegalArgumentException("Component '" + type + "' is unknown!");
        }
        return descriptor.getParameterDescription(page).toString();
    }

    @RequestMapping(value = "{slugSite}/{slugPage}/deleteComponent/{oid}", method = RequestMethod.POST)
    public RedirectView deleteComponent(Model model, @PathVariable(value = "slugSite") String slugSite, @PathVariable(
            value = "slugPage") String slugPage, @PathVariable(value = "oid") String oid) {

        Site s = Site.fromSlug(slugSite);

        AdminSites.canEdit(s);

        Page p = s.pageForSlug(slugPage);
        Component component = p.componentForOid(oid);

        atomic(() -> {
            if (component.getInstalledPageSet().size() == 1) {
                component.delete();
            } else {
                component.removeInstalledPage(p);
            }
        });

        return new RedirectView("/cms/pages/" + s.getSlug() + "/" + p.getSlug() + "/edit", true);
    }
}
