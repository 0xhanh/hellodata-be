///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {environment} from "../../../environments/environment";
import {naviElements} from "../../app-navi-elements";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {BaseComponent} from "../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {TranslocoService} from "@ngneat/transloco";

export const LOGGED_IN_OM_USER = 'logged_in_om_user';

@Component({
  templateUrl: './embedded-data-gov.component.html',
  styleUrls: ['./embedded-data-gov.component.scss']
})
export class EmbeddedDataGovComponent extends BaseComponent implements OnInit {
  baseUrl: string;
  iframeUrl: string = '';
  lang: string = 'en-US';

  // Language mapping from app language to iframe expected format
  private readonly languageMap: Record<string, string> = {
    'de': 'de-CH',
    'en': 'en-US',
    'fr': 'fr-CH',
    'vi': 'vi-VN'
  };

  constructor(private route: ActivatedRoute, private store: Store<AppState>, private translocoService: TranslocoService) {
    super();
    
    // Construct base URL from environment config
    this.baseUrl = environment.subSystemsConfig.dataGov.protocol + environment.subSystemsConfig.dataGov.host
                  + environment.subSystemsConfig.dataGov.domain;
    
    this.iframeUrl = this.baseUrl;
    this.updateLanguage(translocoService.getActiveLang());
    this.createBreadcrumbs();
  }

  private updateLanguage(activeLang: string): void {
    this.lang = this.languageMap[activeLang] || 'en-US';
    this.updateIframeUrl();
  }

  updateIframeUrl() {
    this.iframeUrl = `${this.iframeUrl}?lng=${this.lang}`;
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  private createBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.embeddedDataGov.label,
          routerLink: 'redirect/' + naviElements.embeddedDataGov.path
        }
      ]
    }));
  }

}
