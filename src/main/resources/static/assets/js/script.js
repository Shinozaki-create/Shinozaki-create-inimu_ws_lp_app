(() => {
  'use strict';

  document.addEventListener('DOMContentLoaded', () => {
    const dayNames = ['譌･', '譛・, '轣ｫ', '豌ｴ', '譛ｨ', '驥・, '蝨・];
    const pricePerPerson = 5500;
    const scheduleMap = new Map();
    const slotCache = new Map();

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

    const initialDateKey = reservationDateInput && isDateKey(reservationDateInput.value)
      ? reservationDateInput.value
      : '';

    let currentMonth = initialDateKey ? startOfMonth(parseDateKey(initialDateKey)) : startOfMonth(new Date());
    let currentSelectedDate = initialDateKey || null;
    let reviewCurrentPage = 1;
    let slotRequestId = 0;

    showSections();
    setupHeaderScroll();
    setupPrivacyToggle();
    setupReviews();
    setupFaq();
    setupForm();

    syncSelectedDate(currentSelectedDate);
    void refreshSchedules({ preserveSelection: Boolean(currentSelectedDate) });

    function showSections() {
      document.querySelectorAll(
        '.basic-info--fade, .workshop-flow--fade, .price--fade, .schedule--fade, .shop-info--fade, .asakusa-cta--fade, .reservation-contact--fade, .reviews--fade, .faq--fade'
      ).forEach((section) => section.classList.add('is-visible'));

      faqItems.forEach((item) => item.classList.add('is-content-visible'));
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
      faqItems.forEach((item) => item.classList.add('is-content-visible'));
    }

    function setupReviews() {
      if (!reviewCards.length || !reviewPageButtons.length) return;

      const maxReviewPage = reviewCards.reduce((max, card) => Math.max(max, Number(card.dataset.reviewPage) || 1), 1);

      const updatePagination = (page) => {
        let startPage = page >= 6 ? Math.max(2, page - 4) : 1;
        let endPage = Math.min(maxReviewPage, startPage + 4);

        if (endPage - startPage < 4) {
          startPage = Math.max(1, endPage - 4);
        }

        reviewPageButtons.forEach((button) => {
          const buttonPage = Number(button.dataset.reviewPageButton) || 1;
          const visible = buttonPage >= startPage && buttonPage <= endPage;
          const current = buttonPage === page;
          button.hidden = !visible;
          button.classList.toggle('reviews__page--current', current);
          if (current) button.setAttribute('aria-current', 'page');
          else button.removeAttribute('aria-current');
        });

        if (reviewPrevButton) reviewPrevButton.hidden = page < 6;
        if (reviewNextButton) reviewNextButton.hidden = page >= maxReviewPage;
      };

      const showPage = (page) => {
        reviewCurrentPage = Math.min(Math.max(page, 1), maxReviewPage);
        reviewCards.forEach((card) => {
          const visible = Number(card.dataset.reviewPage) === reviewCurrentPage;
          card.hidden = !visible;
          card.classList.toggle('is-content-visible', visible);
        });
        updatePagination(reviewCurrentPage);
      };

      reviewPageButtons.forEach((button) => {
        button.addEventListener('click', () => showPage(Number(button.dataset.reviewPageButton) || 1));
      });
      if (reviewPrevButton) reviewPrevButton.addEventListener('click', () => showPage(reviewCurrentPage - 1));
      if (reviewNextButton) reviewNextButton.addEventListener('click', () => showPage(reviewCurrentPage + 1));

      showPage(1);
    }

    function setupForm() {
      if (!form) return;

      const bookingFields = Array.from(form.querySelectorAll('.reservation-contact__date-value, .reservation-contact__booking-control'));
      const familyNameInput = form.querySelector('#customer-family-name');
      const givenNameInput = form.querySelector('#customer-given-name');
      const familyKanaInput = form.querySelector('#customer-family-kana');
      const givenKanaInput = form.querySelector('#customer-given-kana');
      const emailInput = form.querySelector('#customer-email');
      const submitButton = form.querySelector('.reservation-contact__submit');

      form.setAttribute('novalidate', 'novalidate');

      const updateMode = () => {
        const inquiryMode = isInquiryMode();
        form.classList.toggle('reservation-contact__form--inquiry-only', inquiryMode);
        bookingFields.forEach((field) => { field.required = !inquiryMode; });
        if (messageTextarea) messageTextarea.required = inquiryMode;
        if (reservationCountSelect) reservationCountSelect.required = !inquiryMode;
        if (reservationTimeSelect) reservationTimeSelect.required = !inquiryMode;
      };

      const clearInvalid = (field) => {
        const wrap = getFieldWrap(field);
        if (wrap) wrap.classList.remove('is-invalid');
      };

      const markInvalid = (field) => {
        const wrap = getFieldWrap(field);
        if (wrap) wrap.classList.add('is-invalid');
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
            if (!inquiryMode && !currentSelectedDate) {
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
            if (!isValidEmailValue(field.value)) {
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
        const inquiryMode = payload.inquiryOnly;
        const dateLabel = inquiryMode ? '縺雁撫縺・粋繧上○縺ｮ縺ｿ' : formatDateKey(payload.reservationDate);
        const timeLabel = inquiryMode ? '縺雁撫縺・粋繧上○縺ｮ縺ｿ' : (selectedSlot ? `${selectedSlot.startTime}縲・{selectedSlot.endTime}` : '譛ｪ驕ｸ謚・);
        const countLabel = inquiryMode ? '縺雁撫縺・粋繧上○縺ｮ縺ｿ' : `${payload.reservationCount}蜷港;
        const totalLabel = inquiryMode ? '窶・ : `ﾂ･${formatCurrency(payload.reservationCount * pricePerPerson)}`;
        const nameLabel = [payload.customerFamilyName, payload.customerGivenName].filter(Boolean).join(' ');
        const kanaLabel = [payload.customerFamilyKana, payload.customerGivenKana].filter(Boolean).join(' ');

        confirmBody.innerHTML = `
          <dl class="reservation-contact__confirm-list">
            ${confirmRow('縺泌ｸ梧悍譌･', dateLabel)}
            ${confirmRow('縺泌ｸ梧悍譎る俣', timeLabel)}
            ${confirmRow('莠ｺ謨ｰ', countLabel)}
            ${confirmRow('蜷郁ｨ磯≡鬘・, totalLabel, 'reservation-contact__confirm-total')}
            ${confirmRow('縺雁錐蜑・, nameLabel)}
            ${confirmRow('縺ｵ繧翫′縺ｪ', kanaLabel)}
            ${confirmRow('繝｡繝ｼ繝ｫ繧｢繝峨Ξ繧ｹ', payload.customerEmail)}
            ${confirmRow('髮ｻ隧ｱ逡ｪ蜿ｷ', payload.customerTel || '窶・)}
            ${confirmRow('縺碑ｦ∵悍', payload.customerMessage || '窶・)}
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
        const message = responseBody && responseBody.message ? responseBody.message : '騾∽ｿ｡繧貞女縺台ｻ倥￠縺ｾ縺励◆縲・;
        const code = responseBody && responseBody.reservationCode ? responseBody.reservationCode : '';
        completeText.innerHTML = escapeHtml(message) + (code ? `<br>莠育ｴ・分蜿ｷ: ${escapeHtml(code)}` : '');
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
          if (typeof invalidField.focus === 'function') {
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
          confirmSubmitButton.textContent = '騾∽ｿ｡荳ｭ...';

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
              throw new Error(responseBody && responseBody.message ? responseBody.message : `騾∽ｿ｡縺ｫ螟ｱ謨励＠縺ｾ縺励◆ (${response.status})`);
            }

            showComplete(responseBody);
            await refreshSchedules({ preserveSelection: true });
          } catch (error) {
            window.alert(error && error.message ? error.message : '騾∽ｿ｡縺ｫ螟ｱ謨励＠縺ｾ縺励◆縲・);
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
        if (!response.ok) throw new Error('髢句ぎ譌･繝・・繧ｿ縺ｮ蜿門ｾ励↓螟ｱ謨励＠縺ｾ縺励◆縲・);
        scheduleMap.clear();
        (Array.isArray(schedules) ? schedules : []).forEach((schedule) => {
          if (schedule && schedule.date) scheduleMap.set(schedule.date, schedule);
        });
      } catch (error) {
        console.error(error);
        scheduleMap.clear();
      }

      slotCache.clear();

      if (!preserveSelection || !currentSelectedDate || !scheduleMap.has(currentSelectedDate) || !isScheduleSelectable(currentSelectedDate)) {
        currentSelectedDate = chooseDefaultDate();
      }

      currentMonth = startOfMonth(parseDateKey(currentSelectedDate || chooseFallbackDate()));
      syncSelectedDate(currentSelectedDate);
      renderCalendars();

      if (currentSelectedDate) {
        await loadSlotsForDate(currentSelectedDate, reservationTimeSelect ? reservationTimeSelect.value : '');
      } else {
        renderEmptySlots();
      }
    }

    function renderCalendars() {
      renderCalendar({
        body: scheduleCalendarBody,
        monthLabel: scheduleMonthLabel,
        caption: scheduleCaption,
        prefix: 'schedule',
        selectedDate: currentSelectedDate,
        monthDate: currentMonth,
        onSelectDate: (dateKey) => void selectDate(dateKey)
      });

      renderCalendar({
        body: reservationCalendarBody,
        monthLabel: reservationMonthLabel,
        caption: reservationCaption,
        prefix: 'reservation-contact',
        selectedDate: currentSelectedDate,
        monthDate: currentMonth,
        onSelectDate: (dateKey) => void selectDate(dateKey)
      });
    }

    async function selectDate(dateKey, preferredTime = '') {
      if (!isDateKey(dateKey)) return;
      currentSelectedDate = dateKey;
      currentMonth = startOfMonth(parseDateKey(dateKey));
      syncSelectedDate(dateKey);
      renderCalendars();
      await loadSlotsForDate(dateKey, preferredTime);
    }

    async function loadSlotsForDate(dateKey, preferredTime = '') {
      if (!dateKey) {
        renderEmptySlots();
        return;
      }

      const requestId = ++slotRequestId;
      if (slotCache.has(dateKey)) {
        if (requestId === slotRequestId && currentSelectedDate === dateKey) {
          renderSlotsAndControls(dateKey, slotCache.get(dateKey) || [], preferredTime);
        }
        return;
      }

      renderSlotLoading();

      try {
        const response = await fetch(`/api/schedules/${encodeURIComponent(dateKey)}/slots`, { headers: { Accept: 'application/json' } });
        const slots = await response.json().catch(() => []);
        if (!response.ok) throw new Error('遨ｺ縺咲憾豕√・蜿門ｾ励↓螟ｱ謨励＠縺ｾ縺励◆縲・);

        const normalizedSlots = Array.isArray(slots) ? slots : [];
        slotCache.set(dateKey, normalizedSlots);
        if (requestId !== slotRequestId || currentSelectedDate !== dateKey) return;
        renderSlotsAndControls(dateKey, normalizedSlots, preferredTime);
      } catch (error) {
        console.error(error);
        if (requestId !== slotRequestId || currentSelectedDate !== dateKey) return;
        renderEmptySlots();
      }
    }

    function renderSlotsAndControls(dateKey, slots, preferredTime = '') {
      renderScheduleSlots(dateKey, slots);
      renderTimeOptions(slots, preferredTime);
    }

    function renderScheduleSlots(dateKey, slots) {
      if (!scheduleSlotList) return;
      if (!slots || !slots.length) {
        scheduleSlotList.innerHTML = `
          <li class="schedule__slot-item">
            <span class="schedule__slot-link schedule__slot-link--disabled" aria-disabled="true">
              <span class="schedule__slot-time">蜿嶺ｻ俶棧縺後≠繧翫∪縺帙ｓ</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">ﾃ・/span>遒ｺ隱堺ｸｭ</span>
              <span class="schedule__slot-arrow" aria-hidden="true">窶ｺ</span>
            </span>
          </li>
        `;
        return;
      }

      scheduleSlotList.innerHTML = slots.map((slot) => {
        const status = getSlotStatus(slot);
        const tag = status.selectable ? 'a' : 'span';
        const href = status.selectable ? ' href="#reservation"' : '';
        const disabledAttrs = status.selectable ? '' : ' aria-disabled="true"';
        const disabledClass = status.selectable ? '' : ' schedule__slot-link--disabled';

        return `
          <li class="schedule__slot-item">
            <${tag}
              class="schedule__slot-link${disabledClass}"
              data-slot-time="${escapeHtml(slot.startTime)}"
              data-slot-date="${escapeHtml(dateKey)}"${href}${disabledAttrs}>
              <span class="schedule__slot-time">${escapeHtml(`${slot.startTime}縲・{slot.endTime}`)}</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">${escapeHtml(status.symbol)}</span>${escapeHtml(status.label)}</span>
              <span class="schedule__slot-arrow" aria-hidden="true">窶ｺ</span>
            </${tag}>
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

    function renderTimeOptions(slots, preferredTime = '') {
      if (!reservationTimeSelect) {
        updateCountOptions(0, '');
        return;
      }

      const previousValue = preferredTime || reservationTimeSelect.value || '';
      reservationTimeSelect.innerHTML = '';

      const defaultOption = document.createElement('option');
      defaultOption.value = '';
      defaultOption.textContent = '驕ｸ謚槭＠縺ｦ縺上□縺輔＞';
      reservationTimeSelect.appendChild(defaultOption);

      if (!slots || !slots.length) {
        reservationTimeSelect.disabled = true;
        reservationTimeSelect.value = '';
        updateCountOptions(0, '');
        return;
      }

      slots.forEach((slot) => {
        const option = document.createElement('option');
        const status = getSlotStatus(slot);
        option.value = slot.startTime;
        option.textContent = `${slot.startTime}縲・{slot.endTime}・・{status.label}・荏;
        option.disabled = !status.selectable;
        reservationTimeSelect.appendChild(option);
      });

      reservationTimeSelect.disabled = false;
      reservationTimeSelect.value = slots.some((slot) => slot.startTime === previousValue && getSlotStatus(slot).selectable)
        ? previousValue
        : '';

      const selectedSlot = getSelectedSlot(slots);
      updateCountOptions(selectedSlot ? Number(selectedSlot.remainingCount || 0) : 0, reservationCountSelect ? reservationCountSelect.value : '');
    }

    function updateCountOptions(maxCount, preferredValue = '') {
      if (!reservationCountSelect) return;
      const inquiryMode = isInquiryMode();
      const countMax = Math.min(Math.max(Number(maxCount) || 0, 0), 10);
      reservationCountSelect.innerHTML = '';

      if (countMax <= 0) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = '貅蟶ｭ縺ｧ縺・;
        reservationCountSelect.appendChild(option);
        reservationCountSelect.disabled = true;
        reservationCountSelect.required = !inquiryMode;
        reservationCountSelect.value = '';
        return;
      }

      const defaultOption = document.createElement('option');
      defaultOption.value = '';
      defaultOption.textContent = '驕ｸ謚槭＠縺ｦ縺上□縺輔＞';
      reservationCountSelect.appendChild(defaultOption);

      for (let count = 1; count <= countMax; count += 1) {
        const option = document.createElement('option');
        option.value = String(count);
        option.textContent = `${count}蜷港;
        reservationCountSelect.appendChild(option);
      }

      reservationCountSelect.disabled = false;
      reservationCountSelect.required = !inquiryMode;
      reservationCountSelect.value = preferredValue && Number(preferredValue) <= countMax ? String(preferredValue) : '';
    }

    function renderEmptySlots() {
      renderScheduleSlots(currentSelectedDate || '', []);
      renderTimeOptions([], '');
    }

    function renderSlotLoading() {
      if (scheduleSlotList) {
        scheduleSlotList.innerHTML = `
          <li class="schedule__slot-item">
            <span class="schedule__slot-link schedule__slot-link--disabled" aria-disabled="true">
              <span class="schedule__slot-time">遨ｺ縺咲憾豕√ｒ隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">窶ｦ</span>遒ｺ隱堺ｸｭ</span>
              <span class="schedule__slot-arrow" aria-hidden="true">窶ｺ</span>
            </span>
          </li>
        `;
      }

      if (reservationTimeSelect) {
        reservationTimeSelect.innerHTML = '<option value="">隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</option>';
        reservationTimeSelect.disabled = true;
      }

      if (reservationCountSelect) {
        reservationCountSelect.innerHTML = '<option value="">隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</option>';
        reservationCountSelect.disabled = true;
      }
    }

    function syncSelectedDate(dateKey) {
      const label = dateKey ? formatDateKey(dateKey) : '窶・;
      if (reservationDateInput) reservationDateInput.value = dateKey || '';
      if (reservationSelectedDateText) reservationSelectedDateText.textContent = label;
      if (scheduleSelectedDateText) scheduleSelectedDateText.textContent = label;
    }

    function chooseDefaultDate() {
      const firstOpen = findFirstSchedule((schedule) => Boolean(schedule.open) && Number(schedule.remainingCount || 0) > 0);
      if (firstOpen) return firstOpen.date;

      const firstOpenAny = findFirstSchedule((schedule) => Boolean(schedule.open));
      if (firstOpenAny) return firstOpenAny.date;

      const firstAny = findFirstSchedule(() => true);
      if (firstAny) return firstAny.date;

      return chooseFallbackDate();
    }

    function chooseFallbackDate() {
      return toDateKey(new Date());
    }

    function findFirstSchedule(predicate) {
      for (const [date, schedule] of scheduleMap.entries()) {
        if (predicate(schedule, date)) return { date, schedule };
      }
      return null;
    }

    function getCalendarStatus(schedule) {
      if (!schedule || !schedule.open) {
        return { open: false, label: '莨第･ｭ譌･', mark: 'ﾃ・ };
      }

      const remaining = Number(schedule.remainingCount || 0);
      const fullyBooked = remaining <= 0 || Boolean(schedule.fullyBooked);
      return { open: true, label: fullyBooked ? '貅蟶ｭ' : '莠育ｴ・庄', mark: fullyBooked ? '貅' : '笳・ };
    }

    function renderCalendar({ body, monthLabel, caption, prefix, selectedDate, monthDate, onSelectDate }) {
      if (!body || !monthLabel || !monthDate) return;

      const year = monthDate.getFullYear();
      const month = monthDate.getMonth();
      const totalDays = new Date(year, month + 1, 0).getDate();
      const firstColumn = (new Date(year, month, 1).getDay() + 6) % 7;

      monthLabel.textContent = formatMonthLabel(monthDate);
      if (caption) caption.textContent = `${formatMonthLabel(monthDate)}縺ｮ髢句ぎ譌･繧ｫ繝ｬ繝ｳ繝繝ｼ`;

      const rows = [];
      let day = 1;

      for (let row = 0; row < 6; row += 1) {
        const cells = [];

        for (let column = 0; column < 7; column += 1) {
          if ((row === 0 && column < firstColumn) || day > totalDays) {
            cells.push(`<td class="${prefix}__calendar-cell ${prefix}__calendar-cell--empty"></td>`);
            continue;
          }

          const date = new Date(year, month, day);
          const dateKey = toDateKey(date);
          const schedule = scheduleMap.get(dateKey);
          const status = getCalendarStatus(schedule, date);
          const isSelected = dateKey === selectedDate;
          const classes = [
            `${prefix}__calendar-cell`,
            status.open ? `${prefix}__calendar-cell--open` : `${prefix}__calendar-cell--closed`,
            isSelected ? `${prefix}__calendar-cell--selected` : ''
          ];

          if (column === 5) classes.push(`${prefix}__calendar-cell--sat`);
          if (column === 6) classes.push(`${prefix}__calendar-cell--sun`);
          if (schedule && schedule.open && column !== 5 && column !== 6) classes.push(`${prefix}__calendar-cell--holiday`);

          const cellClass = classes.filter(Boolean).join(' ');
          const ariaLabel = `${formatDateKey(dateKey)} ${status.label}`;

          if (status.open) {
            cells.push(`
              <td class="${cellClass}">
                <button class="${prefix}__calendar-date" type="button" data-date="${dateKey}" aria-label="${escapeHtml(ariaLabel)}" aria-pressed="${String(isSelected)}">
                  <span class="${prefix}__calendar-number">${day}</span>
                  <span class="${prefix}__calendar-mark">${escapeHtml(status.mark)}</span>
                </button>
              </td>
            `);
          } else {
            cells.push(`
              <td class="${cellClass}">
                <span class="${prefix}__calendar-date" aria-label="${escapeHtml(ariaLabel)}">
                  <span class="${prefix}__calendar-number">${day}</span>
                  <span class="${prefix}__calendar-mark">${escapeHtml(status.mark)}</span>
                </span>
              </td>
            `);
          }

          day += 1;
        }

        rows.push(`<tr class="${prefix}__calendar-row">${cells.join('')}</tr>`);
        if (day > totalDays) break;
      }

      body.innerHTML = rows.join('');
      body.querySelectorAll('[data-date]').forEach((button) => {
        button.addEventListener('click', () => void onSelectDate(button.dataset.date));
      });
    }

    function getSlotStatus(slot) {
      const active = Boolean(slot && slot.active);
      const remaining = Number(slot && slot.remainingCount ? slot.remainingCount : 0);

      if (!active) return { label: '蜿嶺ｻ伜●豁｢', symbol: 'ﾃ・, selectable: false };
      if (remaining <= 0 || slot.fullyBooked) return { label: '貅蟶ｭ', symbol: 'ﾃ・, selectable: false };
      return { label: `谿九ｊ${remaining}蜷港, symbol: remaining <= 3 ? '笆ｳ' : '笳・, selectable: true };
    }

    function getSelectedSlot(slots = getSlotsForCurrentDate()) {
      if (!reservationTimeSelect || !slots.length) return null;
      return slots.find((slot) => slot.startTime === reservationTimeSelect.value && getSlotStatus(slot).selectable) || null;
    }

    function getSlotsForCurrentDate() {
      return currentSelectedDate ? (slotCache.get(currentSelectedDate) || []) : [];
    }

    function isInquiryMode() {
      return Boolean(inquiryCheckbox && inquiryCheckbox.checked);
    }

    function isScheduleSelectable(dateKey) {
      const schedule = scheduleMap.get(dateKey);
      return Boolean(schedule && schedule.open);
    }

    function formatDateKey(dateKey) {
      if (!isDateKey(dateKey)) return String(dateKey || '');
      const date = parseDateKey(dateKey);
      return `${date.getFullYear()}蟷ｴ${date.getMonth() + 1}譛・{date.getDate()}譌･(${dayNames[date.getDay()]})`;
    }

    function formatMonthLabel(date) {
      return `${date.getFullYear()}蟷ｴ${date.getMonth() + 1}譛・;
    }

    function parseDateKey(dateKey) {
      const [year, month, day] = String(dateKey || '').split('-').map((part) => Number(part));
      return new Date(year, month - 1, day);
    }

    function startOfMonth(date) {
      return new Date(date.getFullYear(), date.getMonth(), 1);
    }

    function toDateKey(date) {
      return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0')
      ].join('-');
    }

    function isDateKey(value) {
      return /^\d{4}-\d{2}-\d{2}$/.test(String(value || ''));
    }

    function escapeHtml(value) {
      return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    function formatCurrency(value) {
      return new Intl.NumberFormat('ja-JP').format(value);
    }

    function confirmRow(label, value, extraClass = '') {
      return `
        <div class="reservation-contact__confirm-row${extraClass ? ` ${extraClass}` : ''}">
          <dt class="reservation-contact__confirm-label">${escapeHtml(label)}</dt>
          <dd class="reservation-contact__confirm-value">${escapeHtml(value || '窶・)}</dd>
        </div>
      `;
    }

    function getFieldWrap(field) {
      if (!form || !field) return null;
      if (field === reservationDateInput) return form.querySelector('.reservation-contact__field--calendar');
      if (field === privacyCheckbox) return form.querySelector('.reservation-contact__privacy');
      return field.closest('.reservation-contact__field');
    }

    function isValidEmailValue(value) {
      const trimmed = String(value || '').trim();
      return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(trimmed);
    }

    function buildPayload() {
      const inquiryMode = isInquiryMode();
      const payload = {
        inquiryOnly: inquiryMode,
        customerFamilyName: reservationValue('#customer-family-name'),
        customerGivenName: reservationValue('#customer-given-name'),
        customerFamilyKana: reservationValue('#customer-family-kana'),
        customerGivenKana: reservationValue('#customer-given-kana'),
        customerEmail: reservationValue('#customer-email'),
        customerTel: reservationValue('#customer-tel') || null,
        customerMessage: reservationValue('#customer-message') || null,
        privacyAccepted: Boolean(privacyCheckbox && privacyCheckbox.checked)
      };

      if (!inquiryMode) {
        payload.reservationDate = currentSelectedDate || null;
        payload.reservationTime = reservationTimeSelect ? reservationTimeSelect.value || null : null;
        payload.reservationCount = reservationCountSelect ? Number(reservationCountSelect.value || 0) || null : null;
      }

      return payload;
    }

    function reservationValue(selector) {
      const field = form ? form.querySelector(selector) : null;
      return String(field && field.value ? field.value : '').trim();
    }

    function updateCountOptions(maxCount, preferredValue = '') {
      if (!reservationCountSelect) return;
      const inquiryMode = isInquiryMode();
      const countMax = Math.min(Math.max(Number(maxCount) || 0, 0), 10);
      reservationCountSelect.innerHTML = '';

      if (countMax <= 0) {
        const soldOut = document.createElement('option');
        soldOut.value = '';
        soldOut.textContent = '貅蟶ｭ縺ｧ縺・;
        reservationCountSelect.appendChild(soldOut);
        reservationCountSelect.disabled = true;
        reservationCountSelect.required = !inquiryMode;
        reservationCountSelect.value = '';
        return;
      }

      const defaultOption = document.createElement('option');
      defaultOption.value = '';
      defaultOption.textContent = '驕ｸ謚槭＠縺ｦ縺上□縺輔＞';
      reservationCountSelect.appendChild(defaultOption);

      for (let count = 1; count <= countMax; count += 1) {
        const option = document.createElement('option');
        option.value = String(count);
        option.textContent = `${count}蜷港;
        reservationCountSelect.appendChild(option);
      }

      reservationCountSelect.disabled = false;
      reservationCountSelect.required = !inquiryMode;
      reservationCountSelect.value = preferredValue && Number(preferredValue) <= countMax ? String(preferredValue) : '';
    }

    function renderTimeOptions(slots, preferredTime = '') {
      if (!reservationTimeSelect) {
        updateCountOptions(0, '');
        return;
      }

      const previousValue = preferredTime || reservationTimeSelect.value || '';
      reservationTimeSelect.innerHTML = '';

      const defaultOption = document.createElement('option');
      defaultOption.value = '';
      defaultOption.textContent = '驕ｸ謚槭＠縺ｦ縺上□縺輔＞';
      reservationTimeSelect.appendChild(defaultOption);

      if (!slots || !slots.length) {
        reservationTimeSelect.disabled = true;
        reservationTimeSelect.value = '';
        updateCountOptions(0, '');
        return;
      }

      slots.forEach((slot) => {
        const option = document.createElement('option');
        const status = getSlotStatus(slot);
        option.value = slot.startTime;
        option.textContent = `${slot.startTime}縲・{slot.endTime}・・{status.label}・荏;
        option.disabled = !status.selectable;
        reservationTimeSelect.appendChild(option);
      });

      reservationTimeSelect.disabled = false;
      reservationTimeSelect.value = slots.some((slot) => slot.startTime === previousValue && getSlotStatus(slot).selectable)
        ? previousValue
        : '';

      const selectedSlot = getSelectedSlot(slots);
      updateCountOptions(selectedSlot ? Number(selectedSlot.remainingCount || 0) : 0, reservationCountSelect ? reservationCountSelect.value : '');
    }

    function renderScheduleSlots(dateKey, slots) {
      if (!scheduleSlotList) return;
      if (!slots || !slots.length) {
        scheduleSlotList.innerHTML = `
          <li class="schedule__slot-item">
            <span class="schedule__slot-link schedule__slot-link--disabled" aria-disabled="true">
              <span class="schedule__slot-time">蜿嶺ｻ俶棧縺後≠繧翫∪縺帙ｓ</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">ﾃ・/span>遒ｺ隱堺ｸｭ</span>
              <span class="schedule__slot-arrow" aria-hidden="true">窶ｺ</span>
            </span>
          </li>
        `;
        return;
      }

      scheduleSlotList.innerHTML = slots.map((slot) => {
        const status = getSlotStatus(slot);
        const tag = status.selectable ? 'a' : 'span';
        const href = status.selectable ? ' href="#reservation"' : '';
        const disabledAttrs = status.selectable ? '' : ' aria-disabled="true"';
        const disabledClass = status.selectable ? '' : ' schedule__slot-link--disabled';
        return `
          <li class="schedule__slot-item">
            <${tag}
              class="schedule__slot-link${disabledClass}"
              data-slot-time="${escapeHtml(slot.startTime)}"
              data-slot-date="${escapeHtml(dateKey)}"${href}${disabledAttrs}>
              <span class="schedule__slot-time">${escapeHtml(`${slot.startTime}縲・{slot.endTime}`)}</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">${escapeHtml(status.symbol)}</span>${escapeHtml(status.label)}</span>
              <span class="schedule__slot-arrow" aria-hidden="true">窶ｺ</span>
            </${tag}>
          </li>
        `;
      }).join('');

      scheduleSlotList.querySelectorAll('a[data-slot-time]').forEach((link) => {
        link.addEventListener('click', (event) => {
          event.preventDefault();
          void selectDate(link.dataset.slotDate || dateKey, link.dataset.slotTime || '');
          if (reservationSection) reservationSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
      });
    }

    function renderEmptySlots() {
      renderScheduleSlots(currentSelectedDate || '', []);
      renderTimeOptions([], '');
    }

    function renderSlotLoading() {
      if (scheduleSlotList) {
        scheduleSlotList.innerHTML = `
          <li class="schedule__slot-item">
            <span class="schedule__slot-link schedule__slot-link--disabled" aria-disabled="true">
              <span class="schedule__slot-time">遨ｺ縺咲憾豕√ｒ隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</span>
              <span class="schedule__slot-divider" aria-hidden="true"></span>
              <span class="schedule__slot-status"><span class="schedule__slot-symbol">窶ｦ</span>遒ｺ隱堺ｸｭ</span>
              <span class="schedule__slot-arrow" aria-hidden="true">窶ｺ</span>
            </span>
          </li>
        `;
      }

      if (reservationTimeSelect) {
        reservationTimeSelect.innerHTML = '<option value="">隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</option>';
        reservationTimeSelect.disabled = true;
      }

      if (reservationCountSelect) {
        reservationCountSelect.innerHTML = '<option value="">隱ｭ縺ｿ霎ｼ縺ｿ荳ｭ...</option>';
        reservationCountSelect.disabled = true;
      }
    }

    function getSelectedSlot(slots = getSlotsForCurrentDate()) {
      if (!reservationTimeSelect || !slots.length) return null;
      return slots.find((slot) => slot.startTime === reservationTimeSelect.value && getSlotStatus(slot).selectable) || null;
    }

    function getSlotsForCurrentDate() {
      return currentSelectedDate ? (slotCache.get(currentSelectedDate) || []) : [];
    }

    function isDateKey(value) {
      return /^\d{4}-\d{2}-\d{2}$/.test(String(value || ''));
    }

    function parseDateKey(dateKey) {
      const [year, month, day] = String(dateKey || '').split('-').map((part) => Number(part));
      return new Date(year, month - 1, day);
    }

    function toDateKey(date) {
      return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0')
      ].join('-');
    }

    function startOfMonth(date) {
      return new Date(date.getFullYear(), date.getMonth(), 1);
    }

    function formatMonthLabel(date) {
      return `${date.getFullYear()}蟷ｴ${date.getMonth() + 1}譛・;
    }

    function formatDateKey(dateKey) {
      if (!isDateKey(dateKey)) return String(dateKey || '');
      const date = parseDateKey(dateKey);
      return `${date.getFullYear()}蟷ｴ${date.getMonth() + 1}譛・{date.getDate()}譌･(${dayNames[date.getDay()]})`;
    }

    function formatCurrency(value) {
      return new Intl.NumberFormat('ja-JP').format(value);
    }

    function escapeHtml(value) {
      return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    function isValidEmailValue(value) {
      const trimmed = String(value || '').trim();
      return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(trimmed);
    }

    function isInquiryMode() {
      return Boolean(inquiryCheckbox && inquiryCheckbox.checked);
    }

    function getFieldWrap(field) {
      if (!form || !field) return null;
      if (field === reservationDateInput) return form.querySelector('.reservation-contact__field--calendar');
      if (field === privacyCheckbox) return form.querySelector('.reservation-contact__privacy');
      return field.closest('.reservation-contact__field');
    }

    function confirmRow(label, value, extraClass = '') {
      return `
        <div class="reservation-contact__confirm-row${extraClass ? ` ${extraClass}` : ''}">
          <dt class="reservation-contact__confirm-label">${escapeHtml(label)}</dt>
          <dd class="reservation-contact__confirm-value">${escapeHtml(value || '窶・)}</dd>
        </div>
      `;
    }

    function syncSelectedDate(dateKey) {
      const label = dateKey ? formatDateKey(dateKey) : '窶・;
      if (reservationDateInput) reservationDateInput.value = dateKey || '';
      if (reservationSelectedDateText) reservationSelectedDateText.textContent = label;
      if (scheduleSelectedDateText) scheduleSelectedDateText.textContent = label;
    }

    function chooseDefaultDate() {
      const firstOpen = findFirstSchedule((schedule) => Boolean(schedule.open) && Number(schedule.remainingCount || 0) > 0);
      if (firstOpen) return firstOpen.date;
      const anyOpen = findFirstSchedule((schedule) => Boolean(schedule.open));
      if (anyOpen) return anyOpen.date;
      const any = findFirstSchedule(() => true);
      if (any) return any.date;
      return chooseFallbackDate();
    }

    function chooseFallbackDate() {
      return toDateKey(new Date());
    }

    function findFirstSchedule(predicate) {
      for (const [date, schedule] of scheduleMap.entries()) {
        if (predicate(schedule, date)) return { date, schedule };
      }
      return null;
    }

  });
})();
