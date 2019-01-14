/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output, SimpleChange,
  SimpleChanges,
  ViewChild
} from "@angular/core";
import { isNullOrUndefined } from "util";

@Component({
  selector: 'component-input',
  templateUrl: './input.component.html'
})
export class InputComponent implements OnInit, OnDestroy {

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Private Variables
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  @ViewChild('inputElm')
  private _inputElm: ElementRef;

  @ViewChild('styleElm')
  private _styleElm: ElementRef;

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Protected Variables
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Public Variables
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  @Input() public compType: 'default' | 'apply' | 'search' = 'default';  // 컴포넌트 타입

  @Input() public value: number | string;  // 입력값

  @Input() public valueType: 'string' | 'number' = 'string'; // 입력값 형식

  @Input() public maxLen:number = 1000;         // 최대 입력 길이

  @Input() public placeHolder: string = '';

  @Input() public disabled: boolean = false;   // 입력 비활성 여부

  @Input() public immediately:boolean = false; // 즉시 값 적용 여부

  @Input() public showClear:boolean = true;    // Clear 버튼 표시 여부 ( 현재는 search 에서만 표시 )

  @Input() public inputClass:string = ''; // Input Element 클래스

  @Input() public optionalClass:string = '';   // 추가적인 스타일 적용을 위한 클래스

  @Input() public optionalStyle:string = '';   // 추가적인 스타일 적용을 위한 스타일

  @Output('changeValue') public changeEvent: EventEmitter<number | string> = new EventEmitter();

  @Output('pressEnter') public pressEnterEvent: EventEmitter<boolean> = new EventEmitter();

  @Output('inputFocus') public inputFocusEvent: EventEmitter<boolean> = new EventEmitter();

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Constructor
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

  // 생성자
  constructor(protected elementRef: ElementRef) {
  }

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Override Method
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

  /**
   * 컴포넌트 초기 실행
   */
  public ngOnInit() {
  } // function - ngOnInit

  /**
   * ngOnChanges
   * @param {SimpleChanges} changes
   */
  public ngOnChanges(changes: SimpleChanges) {
    const valueChanges: SimpleChange = changes.value;
    const disabledChanges: SimpleChange = changes.disabled;
    if( this._inputElm ) {
      if( valueChanges && !isNullOrUndefined( valueChanges.currentValue ) ) {
        // this.valueModel = initialChanges.currentValue;
        this._inputElm.nativeElement.value = valueChanges.currentValue;
      }

      if( disabledChanges && !isNullOrUndefined( disabledChanges.currentValue ) ) {
        this._inputElm.nativeElement.disabled = disabledChanges.currentValue;
      }
    }
  } // function - ngOnChanges

  /**
   * 화면 초기화
   */
  public ngAfterViewInit() {
    const inputNativeElm = this._inputElm.nativeElement;
    inputNativeElm.disabled = this.disabled;
    inputNativeElm.value = this.value;
    inputNativeElm.maxlength = this.maxLen;

    if( '' !== this.optionalStyle ) {
      this._styleElm.nativeElement.style = this.optionalStyle;
    }
    if( '' === this.inputClass ) {
      switch (this.compType) {
        case 'apply' :
          this.inputClass = 'ddp-input-txt';
          break;
        case 'default' :
          this.inputClass = 'ddp-data-input';
          break;
      }
    }
  } // function - ngAfterViewInit

  /**
   * 컴포넌트 제거
   */
  public ngOnDestroy() {
  } // function - ngOnDestroy

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Public Method
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Protected Method
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  /**
   * 키 입력 핸들러
   * @param event
   */
  protected keyupHandler(event: KeyboardEvent) {
    if (this.immediately || 13 === event.keyCode) {
      // 즉시 적용 및 Enter 적용
      this.setValue();
      if( 13 === event.keyCode ) {
        this.pressEnterEvent.emit( true );
      }
    } else if( 27 === event.keyCode ) {
      // ESC
      this.resetValue();
    }
  } // function - keyupHandler

  /**
   * in
   */
  protected focusHandler() {
    this.inputFocusEvent.emit();
  } // function - focusHandler

  /**
   * 값 적용
   */
  protected setValue() {
    let inputValue = this._inputElm.nativeElement.value;
    inputValue = inputValue ? inputValue.trim() : '';
    if( inputValue !== this.value ) {
      if( 'string' === this.valueType || ( 'number' === this.valueType && /^[0-9]*$/gi.test(inputValue) ) ) {
        this.value = inputValue;
        this.changeEvent.emit(this.value);
      }
    }
  } // function - setValue

  /**
   * 값 적용 취소
   */
  protected resetValue() {
    this._inputElm.nativeElement.value = this.value;
  } // function - resetValue

  /**
   * 값 제거
   */
  protected clearValue() {
    this._inputElm.nativeElement.value = '';
    this.value = '';
    this.changeEvent.emit(this.value);
  } // function - clearValue

  /**
   * 값 입력 여부
   * @return {boolean}
   */
  protected isNotEmptyInput():boolean {
    if( this._inputElm ) {
      return '' !== this._inputElm.nativeElement.value;
    } else {
      return false;
    }
  } // function - isNotEmptyInput

  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  | Private Method
  |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

}