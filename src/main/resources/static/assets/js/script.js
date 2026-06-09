(() => {
  'use strict';

  document.addEventListener('DOMContentLoaded', () => {
    const WEEKDAY_NAMES = ['日', '月', '火', '水', '木', '金', '土'];
    const PRICE_PER_PERSON = 5500;
    const MAX_RESERVATION_COUNT = 10;
    const LOW_AVAILABILITY_THRESHOLD = 5;

    const scheduleMap = new Map();
    const slotCache = new Map();

    const animatedSections = Array.from(document.querySelectorAll(
      '.basic-info--fade, .workshop-flow--fade, .price--fade, .schedule--fade, .shop-info--fade, .reviews--fade, .faq--fade, .reservation-contact--fade, .asakusa-cta--fade'
    ));

    const siteHeader = document.querySelector('.site-header');
    const reservationSection = document.getElementById('reservation');

    const form = document.querySelector('.reservation-contact__form');
    const inquiryCheckbox = form ? form.querySelector('.reservation-contact__checkbox') : null;
    const privacyCheckbox = form ? form.querySelector('[name="privacy"]') : null;
    const privacyLink = document.querySelector('.reservation-contact__privacy-link');
    const privacyContent = document.getElementById('privacy-policy-content');

    const reservationDateInput = form ? form.querySelector('.reservation-contact__date-value') : null;
    const reservationTimeSelect = form ? form.querySelector('#reservation-time') : null;
    const reservationCountSelect = form ? form.querySelector('#reservation-count') : null;
    const reservationSelectedDateText = form ? form.querySelector('[data-selected-date]') : null;
    const reservationCalendarBody = form ? form.querySelector('[data-reservation-calendar-body]') : null;
    const reservationMonthLabel = form ? form.querySelector('[data-reservation-month]') : null;
    const reservationCaption = form ? form.querySelector('[data-reservation-caption]') : null;
    const reservationPrev = form ? form.querySelector('[data-reservation-prev]') : null;
    const reservationNext = form ? form.querySelector('[data-reservation-next]') : null;
    const messageTextarea = form ? form.querySelector('#customer-message') : null;

    const scheduleMonthLabel = document.querySelector('[data-schedule-month]');
    const scheduleCaption = document.querySelector('[data-schedule-caption]');
    const scheduleCalendarBody = document.querySelector('[data-schedule-calendar-body]');
    const schedulePrev = document.querySelector('[data-schedule-prev]');
    const scheduleNext = document.querySelector('[data-schedule-next]');
    const scheduleSelectedDateText = document.querySelector('[data-schedule-selected-date]');
    const scheduleSlotList = document.querySelector('[data-schedule-slot-list]');

    const confirmSection = document.querySelector('.reservation-contact__confirm');
    const confirmBody = document.querySelector('[data-confirm-body]');
    const confirmBackButton = document.querySelector('[data-confirm-back]');
    const confirmSubmitButton = document.querySelector('[data-confirm-submit]');
    const completeSection = document.querySelector('.reservation-contact__complete');
    const completeText = completeSection ? completeSection.querySelector('.reservation-contact__complete-text') : null;

    const reviewCards = Array.from(document.querySelectorAll('[data-review-page]'));
    const reviewPageButtons = Array.from(document.querySelectorAll('[data-review-page-button]'));
    const reviewPrevButton = document.querySelector('[data-review-prev]');
    const reviewNextButton = document.querySelector('[data-review-next]');
    const faqItems = Array.from(document.querySelectorAll('.faq__item'));

    const initialDateKey = isDateKey(reservationDateInput?.value || '') ? reservationDateInput.value : '';

    let selectedDateKey = initialDateKey || null;
    let viewMonth = selectedDateKey ? startOfMonth(parseDateKey(selectedDateKey)) : startOfMonth(new Date());
    let minScheduleMonth = startOfMonth(new Date());
    let maxScheduleMonth = startOfMonth(new Date());
    let reviewCurrentPage = 1;
    let slotRequestToken = 0;

    setupSectionReveal();
    setupHeaderScroll();
    setupPrivacyToggle();
    setupFaq();
    setupReviews();
    setupForm();
    setupMonthNavigation();

    syncSelectedDate(selectedDateKey);
    updateMonthControls();
    void refreshSchedules({ preserveSelection: true });

    function setupSectionReveal() {
      if (!animatedSections.length) return;

      const reveal = (section) => section.classList.add('is-visible');
      const unrevealed = new Set(animatedSections);
      let queued = false;

      const revealVisibleSections = () => {
        unrevealed.forEach((section) => {
          if (isElementInViewport(section)) {
            reveal(section);
            unrevealed.delete(section);
          }
        });
      };

      const scheduleRevealCheck = () => {
        if (queued) return;
        queued = true;
        window.requestAnimationFrame(() => {
          queued = false;
          revealVisibleSections();
        });
      };

      if (!('IntersectionObserver' in window)) {
        animatedSections.forEach(reveal);
        return;
      }

      const observer = new IntersectionObserver((entries, io) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          reveal(entry.target);
          unrevealed.delete(entry.target);
          io.unobserve(entry.target);
        });
      }, {
        root: null,
        rootMargin: '0px 0px -12% 0px',
        threshold: 0.16
      });

      animatedSections.forEach((section) => observer.observe(section));

      window.requestAnimationFrame(() => {
        revealVisibleSections();
      });

      window.addEventListener('scroll', scheduleRevealCheck, { passive: true });
      window.addEventListener('resize', scheduleRevealCheck, { passive: true });
      window.addEventListener('load', scheduleRevealCheck, { once: true });
    }

    function setupHeaderScroll() {
      if (!siteHeader) return;

      const update = () => siteHeader.classList.toggle('is-scrolled', window.scrollY > 0);
      update();
      window.addEventListener('scroll', update, { passive: true });
    }

    function setupPrivacyToggle() {
      if (!privacyLink || !privacyContent) return;

      privacyLink.addEventListener('click', () => {
        const willShow = privacyContent.hidden;
        privacyContent.hidden = !willShow;
        privacyLink.setAttribute('aria-expanded', String(willShow));
      });
    }

    function setupFaq() {
      faqItems.forEach((item, index) => {
        item.style.setProperty('--faq-item-delay', `${index * 0.12}s`);
        item.classList.add('is-content-visible');
      });
    }

    function setupReviews() {
      if (!reviewCards.length || !reviewPageButtons.length) return;

      const maxReviewPage = reviewCards.reduce((max, card) => {
        const page = Number(card.dataset.reviewPage) || 1;
        return Math.max(max, page);
      }, 1);

      const pageCounts = new Map();
      reviewCards.forEach((card) => {
        const page = Number(card.dataset.reviewPage) || 1;
        const currentIndex = pageCounts.get(page) || 0;
        card.style.setProperty('--reviews-card-delay', `${currentIndex * 0.12}s`);
        pageCounts.set(page, currentIndex + 1);
      });

      const updatePagination = (page) => {
        const windowSize = 5;
        let startPage = Math.max(1, page - 2);
        let endPage = startPage + windowSize - 1;

        if (endPage > maxReviewPage) {
          endPage = maxReviewPage;
          startPage = Math.max(1, endPage - windowSize + 1);
        }

        reviewPageButtons.forEach((button) => {
          const buttonPage = Number(button.dataset.reviewPageButton) || 1;
          const visible = buttonPage >= startPage && buttonPage <= endPage;
          button.hidden = !visible;
          button.classList.toggle('reviews__page--current', buttonPage === page);
          if (buttonPage === page) {
            button.setAttribute('aria-current', 'page');
          } else {
            button.removeAttribute('aria-current');
          }
        });

        if (reviewPrevButton) reviewPrevButton.hidden = page <= 1;
        if (reviewNextButton) reviewNextButton.hidden = page >= maxReviewPage;
      };

      const showPage = (page) => {
        reviewCurrentPage = clamp(page, 1, maxReviewPage);
        reviewCards.forEach((card) => {
          const visible = Number(card.dataset.reviewPage) === reviewCurrentPage;
          card.hidden = !visible;
          card.classList.toggle('is-content-visible', visible);
        });
        updatePagination(reviewCurrentPage);
      };

      reviewPageButtons.forEach((button) => {
        button.addEventListener('click', () => {
          showPage(Number(button.dataset.reviewPageButton) || 1);
        });
      });

      if (reviewPrevButton) {
        reviewPrevButton.addEventListener('click', () => showPage(reviewCurrentPage - 1));
      }

      if (reviewNextButton) {
        reviewNextButton.addEventListener('click', () => showPage(reviewCurrentPage + 1));
      }

      showPage(1);
    }

    function setupMonthNavigation() {
      const goMonth = (delta) => {
        const candidate = addMonths(viewMonth, delta);
        if (compareMonths(candidate, minScheduleMonth) < 0 || compareMonths(candidate, maxScheduleMonth) > 0) {
          return;
        }
        viewMonth = candidate;
        renderCalendars();
      };

      if (schedulePrev) schedulePrev.addEventListener('click', () => goMonth(-1));
      if (scheduleNext) scheduleNext.addEventListener('click', () => goMonth(1));
      if (reservationPrev) reservationPrev.addEventListener('click', () => goMonth(-1));
      if (reservationNext) reservationNext.addEventListener('click', () => goMonth(1));
    }

    function setupForm() {
      if (!form) return;

      const bookingFields = [reservationDateInput, reservationTimeSelect, reservationCountSelect].filter(Boolean);
      const familyNameInput = form.querySelector('#customer-family-name');
      const givenNameInput = form.querySelector('#customer-given-name');
      const familyKanaInput = form.querySelector('#customer-family-kana');
      const givenKanaInput = form.querySelector('#customer-given-kana');
      const emailInput = form.querySelector('#customer-email');

      form.setAttribute('novalidate', 'novalidate');

      const updateMode = () => {
        const inquiryMode = isInquiryMode();
        form.classList.toggle('reservation-contact__form--inquiry-only', inquiryMode);

        bookingFields.forEach((field) => {
          field.required = !inquiryMode;
        });

        if (messageTextarea) messageTextarea.required = inquiryMode;
        if (reservationTimeSelect) reservationTimeSelect.required = !inquiryMode;
        if (reservationCountSelect) reservationCountSelect.required = !inquiryMode;
      };

      const validate = () => {
        const inquiryMode = isInquiryMode();
        const fields = [
          familyNameInput,
          givenNameInput,
          familyKanaInput,
          givenKanaInput,
          emailInput,
          messageTextarea,
          reservationDateInput,
          reservationTimeSelect,
          reservationCountSelect,
          privacyCheckbox
        ].filter(Boolean);

        for (const field of fields) {
          clearInvalid(field);

          if (field === privacyCheckbox) {
            if (!field.checked) {
              markInvalid(field);
              return field;
            }
            continue;
          }

          if (field === reservationDateInput) {
            if (!inquiryMode && !selectedDateKey) {
              markInvalid(field);
              return field;
            }
            continue;
          }

          if (field === reservationTimeSelect) {
            if (!inquiryMode && (!field.value || field.disabled)) {
              markInvalid(field);
              return field;
            }
            continue;
          }

          if (field === reservationCountSelect) {
            if (!inquiryMode && (!field.value || field.disabled)) {
              markInvalid(field);
              return field;
            }
            continue;
          }

          if (field === messageTextarea) {
            if (inquiryMode && !String(field.value || '').trim()) {
              markInvalid(field);
              return field;
            }
            continue;
          }

          if (field === emailInput) {
            if (!field.checkValidity()) {
              markInvalid(field);
              return field;
            }
            continue;
          }

          if (!String(field.value || '').trim()) {
            markInvalid(field);
            return field;
          }
        }

        return null;
      };

      const showConfirm = () => {
        if (!confirmSection || !confirmBody) return;

        const payload = buildPayload();
        const selectedSlot = getSelectedSlot();
        const inquiryMode = payload.inquiry_only;
        const dateLabel = inquiryMode ? 'お問い合わせのみ' : formatDateLabel(payload.reservation_date);
        const timeLabel = inquiryMode
          ? 'お問い合わせのみ'
          : (selectedSlot ? `${selectedSlot.startTime} ～ ${selectedSlot.endTime}` : '未選択');
        const countLabel = inquiryMode ? 'お問い合わせのみ' : `${payload.reservation_count}名`;
        const totalLabel = inquiryMode ? '-' : `${formatMoney(payload.reservation_count * PRICE_PER_PERSON)}円`;
        const nameLabel = [payload.customer_family_name, payload.customer_given_name].filter(Boolean).join(' ');
        const kanaLabel = [payload.customer_family_kana, payload.customer_given_kana].filter(Boolean).join(' ');

        confirmBody.innerHTML = `
          <dl class="reservation-contact__confirm-list">
            ${confirmRow('予約日', dateLabel)}
            ${confirmRow('予約時間', timeLabel)}
            ${confirmRow('人数', countLabel)}
            ${confirmRow('合計', totalLabel, 'reservation-contact__confirm-total')}
            ${confirmRow('お名前', nameLabel)}
            ${confirmRow('フリガナ', kanaLabel)}
            ${confirmRow('メールアドレス', payload.customer_email)}
            ${confirmRow('電話番号', payload.customer_tel || '-')}
            ${confirmRow('メッセージ', payload.customer_message || '-')}
          </dl>
        `;

        if (completeSection) completeSection.hidden = true;
        form.hidden = true;
        confirmSection.hidden = false;
        confirmSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      };

      const showForm = () => {
        if (confirmSection) confirmSection.hidden = true;
        if (completeSection) completeSection.hidden = true;
        form.hidden = false;
        form.scrollIntoView({ behavior: 'smooth', block: 'start' });
      };

      const showComplete = (responseBody) => {
        if (!completeSection || !completeText) return;

        const message = responseBody && responseBody.message
          ? responseBody.message
          : '送信が完了しました。';
        const code = responseBody && responseBody.reservationCode ? responseBody.reservationCode : '';

        completeText.innerHTML = escapeHtml(message) + (code ? `<br>受付番号: ${escapeHtml(code)}` : '');

        if (confirmSection) confirmSection.hidden = true;
        form.hidden = true;
        completeSection.hidden = false;
        completeSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      };

      form.addEventListener('submit', (event) => {
        event.preventDefault();

        const invalidField = validate();
        if (invalidField) {
          const target = getFieldWrap(invalidField) || invalidField;
          target.scrollIntoView({ behavior: 'smooth', block: 'center' });
          if (typeof invalidField.focus === 'function' && invalidField.type !== 'hidden') {
            invalidField.focus({ preventScroll: true });
          }
          return;
        }

        showConfirm();
      });

      if (confirmBackButton) {
        confirmBackButton.addEventListener('click', showForm);
      }

      if (confirmSubmitButton) {
        confirmSubmitButton.addEventListener('click', async () => {
          const originalText = confirmSubmitButton.textContent;
          confirmSubmitButton.disabled = true;
          confirmSubmitButton.textContent = '送信中...';

          try {
            const response = await fetch('/api/reservations', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
              },
              body: JSON.stringify(buildPayload())
            });

            const responseBody = await response.json().catch(() => null);
            if (!response.ok) {
              throw new Error(responseBody && responseBody.message
                ? responseBody.message
                : `送信に失敗しました（${response.status}）`);
            }

            showComplete(responseBody);
            await refreshSchedules({ preserveSelection: true });
          } catch (error) {
            window.alert(error && error.message ? error.message : '送信に失敗しました。');
          } finally {
            confirmSubmitButton.disabled = false;
            confirmSubmitButton.textContent = originalText;
          }
        });
      }

      if (inquiryCheckbox) {
        inquiryCheckbox.addEventListener('change', () => {
          updateMode();
          updateCountOptions(currentAvailableSeatCount(), reservationCountSelect ? reservationCountSelect.value : '');
        });
      }

      if (reservationTimeSelect) {
        reservationTimeSelect.addEventListener('change', () => {
          clearInvalid(reservationTimeSelect);
          updateCountOptions(currentAvailableSeatCount(), reservationCountSelect ? reservationCountSelect.value : '');
        });
      }

      if (reservationCountSelect) {
        reservationCountSelect.addEventListener('change', () => clearInvalid(reservationCountSelect));
      }

      if (messageTextarea) {
        messageTextarea.addEventListener('input', () => clearInvalid(messageTextarea));
      }

      form.querySelectorAll('input, select, textarea').forEach((field) => {
        field.addEventListener('input', () => clearInvalid(field));
        field.addEventListener('change', () => clearInvalid(field));
      });

      updateMode();
    }

    async function refreshSchedules({ preserveSelection = false } = {}) {
      try {
        const response = await fetch('/api/schedules', { headers: { Accept: 'application/json' } });
        const schedules = await response.json().catch(() => []);

        if (!response.ok) {
          throw new Error('スケジュールデータの読み込みに失敗しました。');
        }

        scheduleMap.clear();
        (Array.isArray(schedules) ? schedules : []).forEach((schedule) => {
          if (schedule && schedule.date) {
            scheduleMap.set(schedule.date, schedule);
          }
        });
      } catch (error) {
        console.error(error);
        scheduleMap.clear();
      }

      slotCache.clear();
      updateScheduleBounds();

      selectedDateKey = chooseSelectedDate(preserveSelection);
      if (selectedDateKey) {
        viewMonth = startOfMonth(parseDateKey(selectedDateKey));
      }

      syncSelectedDate(selectedDateKey);
      renderCalendars();

      if (selectedDateKey) {
        await loadSlotsForDate(selectedDateKey, reservationTimeSelect ? reservationTimeSelect.value : '');
      } else {
        renderEmptySlots('日付を選択してください。');
        renderTimeOptions([], null, '');
        updateCountOptions(0, '');
      }
    }

    function updateScheduleBounds() {
      const orderedDates = Array.from(scheduleMap.keys()).sort();
      if (!orderedDates.length) {
        minScheduleMonth = startOfMonth(viewMonth);
        maxScheduleMonth = startOfMonth(viewMonth);
        return;
      }

      minScheduleMonth = startOfMonth(parseDateKey(orderedDates[0]));
      maxScheduleMonth = startOfMonth(parseDateKey(orderedDates[orderedDates.length - 1]));
    }

    function chooseSelectedDate(preserveSelection) {
      if (preserveSelection && selectedDateKey && scheduleMap.has(selectedDateKey)) {
        return selectedDateKey;
      }

      const orderedDates = Array.from(scheduleMap.keys()).sort();
      if (!orderedDates.length) {
        return selectedDateKey;
      }

      return orderedDates[0];
    }

    function renderCalendars() {
      const renderedMonth = startOfMonth(viewMonth);

      renderCalendar({
        body: scheduleCalendarBody,
        monthLabel: scheduleMonthLabel,
        caption: scheduleCaption,
        baseClass: 'schedule',
        monthDate: renderedMonth,
        selectedDateKey,
        onSelectDate: (dateKey) => void selectDate(dateKey, reservationTimeSelect ? reservationTimeSelect.value : '')
      });

      renderCalendar({
        body: reservationCalendarBody,
        monthLabel: reservationMonthLabel,
        caption: reservationCaption,
        baseClass: 'reservation-contact',
        monthDate: renderedMonth,
        selectedDateKey,
        onSelectDate: (dateKey) => void selectDate(dateKey, reservationTimeSelect ? reservationTimeSelect.value : '')
      });

      updateMonthControls();
    }

    function renderCalendar({ body, monthLabel, caption, baseClass, monthDate, selectedDateKey: currentSelectedDate, onSelectDate }) {
      if (!body) return;

      if (monthLabel) {
        monthLabel.textContent = formatMonthLabel(monthDate);
      }

      if (caption) {
        caption.textContent = `${formatMonthLabel(monthDate)}のカレンダー`;
      }

      const year = monthDate.getFullYear();
      const monthIndex = monthDate.getMonth();
      const firstDay = new Date(year, monthIndex, 1);
      const firstWeekday = (firstDay.getDay() + 6) % 7;
      const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
      const rowCount = Math.ceil((firstWeekday + daysInMonth) / 7);

      const rows = [];
      for (let rowIndex = 0; rowIndex < rowCount; rowIndex += 1) {
        const cells = [];
        for (let columnIndex = 0; columnIndex < 7; columnIndex += 1) {
          const cellIndex = rowIndex * 7 + columnIndex;
          if (cellIndex < firstWeekday || cellIndex >= firstWeekday + daysInMonth) {
            cells.push(`<td class="${baseClass}__calendar-cell ${baseClass}__calendar-cell--empty"></td>`);
            continue;
          }

          const dayNumber = cellIndex - firstWeekday + 1;
          const date = new Date(year, monthIndex, dayNumber);
          const dateKey = toDateKey(date);
          const schedule = scheduleMap.get(dateKey) || null;
          const dayState = getCalendarDayState(schedule);
          const isSelected = dateKey === currentSelectedDate;
          const dayClasses = [
            `${baseClass}__calendar-cell`,
            `${baseClass}__calendar-cell--${dayState.cellState}`,
            columnIndex === 5 ? `${baseClass}__calendar-cell--sat` : '',
            columnIndex === 6 ? `${baseClass}__calendar-cell--sun` : '',
            isSelected ? `${baseClass}__calendar-cell--selected` : ''
          ].filter(Boolean).join(' ');

        const ariaLabel = `${formatDateLabel(dateKey)} ${dayState.ariaLabel}`;
        const openCell = dayState.selectable;

          if (openCell) {
            cells.push(`
              <td class="${dayClasses}">
                <button class="${baseClass}__calendar-date" type="button" data-date="${escapeHtml(dateKey)}" aria-pressed="${String(isSelected)}" aria-label="${escapeHtml(ariaLabel)}">
                  <span class="${baseClass}__calendar-number">${dayNumber}</span>
                  <span class="${baseClass}__calendar-mark">${escapeHtml(dayState.mark)}</span>
                </button>
              </td>
            `);
          } else {
            cells.push(`
              <td class="${dayClasses}">
                <span class="${baseClass}__calendar-date" aria-label="${escapeHtml(ariaLabel)}">
                  <span class="${baseClass}__calendar-number">${dayNumber}</span>
                  <span class="${baseClass}__calendar-mark">${escapeHtml(dayState.mark)}</span>
                </span>
              </td>
            `);
          }
        }
        rows.push(`<tr class="${baseClass}__calendar-row">${cells.join('')}</tr>`);
      }

      body.innerHTML = rows.join('');

      body.querySelectorAll('button[data-date]').forEach((button) => {
        button.addEventListener('click', () => {
          onSelectDate(button.dataset.date || '');
        });
      });
    }

    function getCalendarDayState(schedule) {
      if (!schedule) {
        return { selectable: false, cellState: 'closed', mark: '－', ariaLabel: '休業日' };
      }

      if (!schedule.open) {
        return { selectable: false, cellState: 'closed', mark: '－', ariaLabel: '休業日' };
      }

      if (schedule.fullyBooked || (Number(schedule.remainingCount) || 0) <= 0) {
        return { selectable: false, cellState: 'closed', mark: '×', ariaLabel: '満席' };
      }

      return {
        selectable: true,
        cellState: 'open',
        mark: '●',
        ariaLabel: `開催日、残席${schedule.remainingCount}席`
      };
    }

    async function selectDate(dateKey, preferredTime = '') {
      if (!isDateKey(dateKey)) return;

      selectedDateKey = dateKey;
      viewMonth = startOfMonth(parseDateKey(dateKey));
      syncSelectedDate(dateKey);
      renderCalendars();
      clearInvalid(reservationDateInput);

      await loadSlotsForDate(dateKey, preferredTime);
    }

    async function loadSlotsForDate(dateKey, preferredTime = '') {
      if (!dateKey) {
        renderEmptySlots('日付を選択してください。');
        renderTimeOptions([], null, '');
        updateCountOptions(0, '');
        return;
      }

      const schedule = scheduleMap.get(dateKey) || null;
      if (!schedule) {
        renderEmptySlots('日付を選択してください。');
        renderTimeOptions([], null, '');
        updateCountOptions(0, '');
        return;
      }

      const requestToken = ++slotRequestToken;
      if (slotCache.has(dateKey)) {
        if (requestToken === slotRequestToken && selectedDateKey === dateKey) {
          renderSlotsAndControls(dateKey, slotCache.get(dateKey) || [], preferredTime);
        }
        return;
      }

      renderSlotLoading();

      try {
        const response = await fetch(`/api/schedules/${encodeURIComponent(dateKey)}/slots`, {
          headers: { Accept: 'application/json' }
        });
        const slots = await response.json().catch(() => []);

        if (!response.ok) {
          throw new Error('空き状況データの読み込みに失敗しました。');
        }

        const normalizedSlots = Array.isArray(slots) ? slots : [];
        slotCache.set(dateKey, normalizedSlots);

        if (requestToken !== slotRequestToken || selectedDateKey !== dateKey) return;
        renderSlotsAndControls(dateKey, normalizedSlots, preferredTime);
      } catch (error) {
        console.error(error);
        if (requestToken !== slotRequestToken || selectedDateKey !== dateKey) return;
        renderEmptySlots('空き状況データの読み込みに失敗しました。');
        renderTimeOptions([], schedule, '');
        updateCountOptions(0, '');
      }
    }

    function renderSlotsAndControls(dateKey, slots, preferredTime = '') {
      const schedule = scheduleMap.get(dateKey) || null;
      const selectedSlot = renderTimeOptions(slots, schedule, preferredTime);
      renderScheduleSlots(dateKey, slots, schedule, selectedSlot ? selectedSlot.startTime : '');
      updateCountOptions(selectedSlot ? selectedSlot.remainingCount : 0, reservationCountSelect ? reservationCountSelect.value : '');
    }

    function renderScheduleSlots(dateKey, slots, schedule, selectedTime = '') {
      if (!scheduleSlotList) return;

      if (!slots || !slots.length) {
        renderEmptySlots('選択できる時間帯がありません。');
        return;
      }

      scheduleSlotList.innerHTML = slots.map((slot) => {
        const status = getSlotStatus(slot, schedule);
        const tagName = status.selectable ? 'a' : 'span';
        const selected = status.selectable && selectedTime && slot.startTime === selectedTime;
        const linkAttrs = status.selectable
          ? `href="#reservation" data-slot-time="${escapeHtml(slot.startTime)}" data-slot-date="${escapeHtml(dateKey)}"${selected ? ' aria-current="true"' : ''}`
          : 'aria-disabled="true"';
        const disabledClass = status.selectable ? '' : ' schedule__slot-link--disabled';
        const slotLabel = `${slot.startTime} ～ ${slot.endTime}`;

        return `
          <li class="schedule__slot-item">
            <${tagName}
              class="schedule__slot-link${disabledClass}"
              ${linkAttrs}>
              <span class="schedule__slot-time">${escapeHtml(slotLabel)}</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">${escapeHtml(status.symbol)}</span>${escapeHtml(status.label)}</span>
              <span class="schedule__slot-arrow" aria-hidden="true">→</span>
            </${tagName}>
          </li>
        `;
      }).join('');

      scheduleSlotList.querySelectorAll('a[data-slot-time]').forEach((link) => {
        link.addEventListener('click', (event) => {
          event.preventDefault();
          const date = link.dataset.slotDate || dateKey;
          const time = link.dataset.slotTime || '';
          void selectDate(date, time);
          if (reservationSection) {
            reservationSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        });
      });
    }

    function renderTimeOptions(slots, schedule, preferredTime = '') {
      if (!reservationTimeSelect) {
        return null;
      }

      const allSlots = Array.isArray(slots) ? slots : [];
      const selectableSlots = allSlots.filter((slot) => isSlotSelectable(slot, schedule));
      const currentValue = preferredTime || reservationTimeSelect.value || '';
      const nextSlot = selectableSlots.find((slot) => slot.startTime === currentValue)
        || selectableSlots[0]
        || null;

      const options = ['<option value="">時間帯を選択してください</option>'];
      allSlots.forEach((slot) => {
        const status = getSlotStatus(slot, schedule);
        const label = `${slot.startTime} ～ ${slot.endTime}`;
        const optionLabel = `${label}（${status.label}）`;
        options.push(`<option value="${escapeHtml(slot.startTime)}"${status.selectable ? '' : ' disabled'}>${escapeHtml(optionLabel)}</option>`);
      });

      reservationTimeSelect.innerHTML = options.join('');
      reservationTimeSelect.disabled = !selectableSlots.length;
      reservationTimeSelect.value = nextSlot ? nextSlot.startTime : '';
      clearInvalid(reservationTimeSelect);

      return nextSlot;
    }

    function updateCountOptions(maxCount, preferredValue = '') {
      if (!reservationCountSelect) return;

      const availableCount = Math.max(0, Math.min(MAX_RESERVATION_COUNT, Number(maxCount) || 0));
      const currentValue = preferredValue || reservationCountSelect.value || '';

      if (availableCount <= 0) {
        reservationCountSelect.innerHTML = '<option value="">選択できません</option>';
        reservationCountSelect.value = '';
        reservationCountSelect.disabled = true;
        return;
      }

      const options = ['<option value="">人数を選択してください</option>'];
      for (let count = 1; count <= availableCount; count += 1) {
        options.push(`<option value="${count}">${count}名</option>`);
      }

      reservationCountSelect.innerHTML = options.join('');
      reservationCountSelect.disabled = false;
      reservationCountSelect.value = String(
        currentValue && Number(currentValue) >= 1 && Number(currentValue) <= availableCount
          ? Number(currentValue)
          : 1
      );
      clearInvalid(reservationCountSelect);
    }

    function renderSlotLoading() {
      if (!scheduleSlotList) return;
      scheduleSlotList.innerHTML = `
        <li class="schedule__slot-item">
          <span class="schedule__slot-link schedule__slot-link--disabled" aria-disabled="true">
            <span class="schedule__slot-time">読み込み中...</span>
            <span class="schedule__slot-divider" aria-hidden="true"></span>
            <span class="schedule__slot-status"><span class="schedule__slot-symbol">...</span>空き状況を読み込み中</span>
            <span class="schedule__slot-arrow" aria-hidden="true">→</span>
          </span>
        </li>
      `;
    }

    function renderEmptySlots(message) {
      if (!scheduleSlotList) return;
      scheduleSlotList.innerHTML = `
        <li class="schedule__slot-item">
          <span class="schedule__slot-link schedule__slot-link--disabled" aria-disabled="true">
            <span class="schedule__slot-time">${escapeHtml(message)}</span>
            <span class="schedule__slot-divider" aria-hidden="true"></span>
            <span class="schedule__slot-status"><span class="schedule__slot-symbol">－</span>－</span>
            <span class="schedule__slot-arrow" aria-hidden="true">→</span>
          </span>
        </li>
      `;
    }

    function getSlotStatus(slot, schedule) {
      if (!schedule || !schedule.open) {
        return { selectable: false, symbol: '×', label: '空きなし' };
      }

      if (!slot.active) {
        return { selectable: false, symbol: '×', label: '空きなし' };
      }

      if (slot.fullyBooked || (Number(slot.remainingCount) || 0) <= 0) {
        return { selectable: false, symbol: '×', label: '空きなし' };
      }

      if ((Number(slot.remainingCount) || 0) <= LOW_AVAILABILITY_THRESHOLD) {
        return {
          selectable: true,
          symbol: '△',
          label: '残り僅か'
        };
      }

      return {
        selectable: true,
        symbol: '○',
        label: '空きあり'
      };
    }

    function isSlotSelectable(slot, schedule) {
      return getSlotStatus(slot, schedule).selectable;
    }

    function currentAvailableSeatCount() {
      const selectedSlot = getSelectedSlot();
      return selectedSlot ? Number(selectedSlot.remainingCount) || 0 : 0;
    }

    function getSelectedSlot() {
      if (!selectedDateKey || !reservationTimeSelect) return null;
      const slots = slotCache.get(selectedDateKey) || [];
      return slots.find((slot) => slot.startTime === reservationTimeSelect.value) || null;
    }

    function buildPayload() {
      const inquiryMode = isInquiryMode();
      return {
        inquiry_only: inquiryMode,
        reservation_date: inquiryMode ? null : selectedDateKey,
        reservation_time: inquiryMode ? null : (reservationTimeSelect ? reservationTimeSelect.value || null : null),
        reservation_count: inquiryMode ? null : (reservationCountSelect && reservationCountSelect.value ? Number(reservationCountSelect.value) : null),
        customer_family_name: form ? form.querySelector('#customer-family-name')?.value.trim() || '' : '',
        customer_given_name: form ? form.querySelector('#customer-given-name')?.value.trim() || '' : '',
        customer_family_kana: form ? form.querySelector('#customer-family-kana')?.value.trim() || '' : '',
        customer_given_kana: form ? form.querySelector('#customer-given-kana')?.value.trim() || '' : '',
        customer_email: form ? form.querySelector('#customer-email')?.value.trim() || '' : '',
        customer_tel: form ? form.querySelector('#customer-tel')?.value.trim() || '' : '',
        customer_message: messageTextarea ? messageTextarea.value.trim() : '',
        privacy: Boolean(privacyCheckbox && privacyCheckbox.checked)
      };
    }

    function isInquiryMode() {
      return Boolean(inquiryCheckbox && inquiryCheckbox.checked);
    }

    function syncSelectedDate(dateKey) {
      const label = dateKey ? formatDateLabel(dateKey) : '未選択';
      if (reservationDateInput) reservationDateInput.value = dateKey || '';
      if (reservationSelectedDateText) reservationSelectedDateText.textContent = label;
      if (scheduleSelectedDateText) scheduleSelectedDateText.textContent = label;
      if (dateKey) clearInvalid(reservationDateInput);
    }

    function updateMonthControls() {
      const canGoPrev = compareMonths(viewMonth, minScheduleMonth) > 0;
      const canGoNext = compareMonths(viewMonth, maxScheduleMonth) < 0;

      setButtonState(schedulePrev, !canGoPrev);
      setButtonState(reservationPrev, !canGoPrev);
      setButtonState(scheduleNext, !canGoNext);
      setButtonState(reservationNext, !canGoNext);
    }

    function setButtonState(button, disabled) {
      if (!button) return;
      button.disabled = disabled;
      button.setAttribute('aria-disabled', String(disabled));
    }

    function clearInvalid(field) {
      const wrap = getFieldWrap(field);
      if (wrap) wrap.classList.remove('is-invalid');
    }

    function markInvalid(field) {
      const wrap = getFieldWrap(field);
      if (wrap) wrap.classList.add('is-invalid');
    }

    function getFieldWrap(field) {
      if (!field) return null;
      return field.closest('.reservation-contact__field, .reservation-contact__privacy');
    }

    function confirmRow(label, value, extraClass = '') {
      return `
        <div class="reservation-contact__confirm-row ${extraClass}">
          <div class="reservation-contact__confirm-label">${escapeHtml(label)}</div>
          <div class="reservation-contact__confirm-value">${escapeHtml(value)}</div>
        </div>
      `;
    }

    function compareMonths(left, right) {
      const leftValue = left.getFullYear() * 12 + left.getMonth();
      const rightValue = right.getFullYear() * 12 + right.getMonth();
      return leftValue - rightValue;
    }

    function addMonths(date, delta) {
      return startOfMonth(new Date(date.getFullYear(), date.getMonth() + delta, 1));
    }

    function startOfMonth(date) {
      return new Date(date.getFullYear(), date.getMonth(), 1);
    }

    function parseDateKey(dateKey) {
      if (!isDateKey(dateKey)) return null;
      const [year, month, day] = dateKey.split('-').map(Number);
      return new Date(year, month - 1, day);
    }

    function toDateKey(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }

    function isDateKey(value) {
      return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
    }

    function formatDateLabel(dateKey) {
      const date = parseDateKey(dateKey);
      if (!date) return '';
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      return `${year}年${month}月${day}日（${WEEKDAY_NAMES[date.getDay()]}）`;
    }

    function formatMonthLabel(date) {
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      return `${year}年${month}月`;
    }

    function formatMoney(amount) {
      return new Intl.NumberFormat('ja-JP').format(amount);
    }

    function clamp(value, min, max) {
      return Math.min(Math.max(value, min), max);
    }

    function isElementInViewport(element) {
      const rect = element.getBoundingClientRect();
      return rect.top < window.innerHeight * 0.88 && rect.bottom > 0;
    }

    function escapeHtml(value) {
      return String(value ?? '').replace(/[&<>"']/g, (character) => {
        switch (character) {
          case '&':
            return '&amp;';
          case '<':
            return '&lt;';
          case '>':
            return '&gt;';
          case '"':
            return '&quot;';
          case '\'':
            return '&#39;';
          default:
            return character;
        }
      });
    }
  });
})();
