import{l as h,n as D,o as M}from"./index-67wm3rxu.js";import"./frappe-ui-Cee7Yz-T.js";/*!
 * (C) Ionic http://ionicframework.com - MIT License
 */const G=(n,m,g,p,X)=>{const c=n.ownerDocument.defaultView;let o=h(n);const w=t=>{const{startX:e}=t;return o?e>=c.innerWidth-50:e<=50},a=t=>o?-t.deltaX:t.deltaX,v=t=>o?-t.velocityX:t.velocityX;return D({el:n,gestureName:"goback-swipe",gesturePriority:40,threshold:10,canStart:t=>(o=h(n),w(t)&&m()),onStart:g,onMove:t=>{const e=a(t)/c.innerWidth;p(e)},onEnd:t=>{const s=a(t),e=c.innerWidth,r=s/e,i=v(t),y=e/2,l=i>=0&&(i>.2||s>y),u=(l?1-r:r)*e;let d=0;if(u>5){const f=u/Math.abs(i);d=Math.min(f,540)}X(l,r<=0?.01:M(0,r,.9999),d)}})};export{G as createSwipeBackGesture};
//# sourceMappingURL=swipe-back-BjU5gzrH.js.map
