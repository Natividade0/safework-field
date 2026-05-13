(function(){
  const originalPrint = window.print ? window.print.bind(window) : null;

  window.safePrint = function(){
    try {
      if (window.SafeFieldAndroid && typeof window.SafeFieldAndroid.printCurrentPage === 'function') {
        window.SafeFieldAndroid.printCurrentPage();
        return;
      }
    } catch (e) {}

    if (originalPrint) {
      originalPrint();
    } else {
      alert('Impressão/PDF não disponível neste dispositivo.');
    }
  };

  window.print = window.safePrint;
})();
